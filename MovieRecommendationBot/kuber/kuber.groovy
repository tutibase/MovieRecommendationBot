pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "polyalugovenko/movie-recommendation-bot-new-infra"
        IMAGE_TAG = "latest"
        K8S_NAMESPACE = "movie-bot-ns"
        SSH_USER = 'ubuntu'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                echo "✅ Repository checked out"
            }
        }

        stage('Get Infrastructure Info') {
            steps {
                script {
                    copyArtifacts(
                            projectName: 'cloud_infra',
                            selector: lastSuccessful(),
                            filter: 'MovieRecommendationBot/outputs/vm_ip.txt',
                            target: '.',
                            flatten: true
                    )

                    if (fileExists('vm_ip.txt')) {
                        env.VM_IP = sh(script: 'cat vm_ip.txt', returnStdout: true).trim()
                        if (env.VM_IP) {
                            echo "🌐 Target VM IP: ${env.VM_IP}"
                        } else {
                            error("❌ vm_ip.txt is empty")
                        }
                    } else {
                        error("❌ Could not find vm_ip.txt from cloud_infra artifacts")
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([
                        sshUserPrivateKey(
                                credentialsId: 'ssh-private-key',  // ← ваш SSH-ключ из предыдущих лаб
                                keyFileVariable: 'SSH_KEY_FILE',
                                usernameVariable: 'SSH_USER_VAR',
                                passphraseVariable: ''
                        ),
                        file(credentialsId: 'app-env-content', variable: 'ENV_FILE_PATH')
                ]) {
                    sh """
                echo "🔗 Connecting to VM ${env.VM_IP}..."
                
                # 🔧 Функция для выполнения команд kubectl через SSH
                run_k8s() {
                    ssh -i \${SSH_KEY_FILE} \\
                        -o StrictHostKeyChecking=no \\
                        -o ConnectTimeout=10 \\
                        ${SSH_USER}@${env.VM_IP} "\$*"
                }
                
                NAMESPACE="${K8S_NAMESPACE}"
                
                echo "🔗 Testing connection..."
                run_k8s "kubectl cluster-info"
                
                # Создаём namespace
                run_k8s "kubectl create namespace \${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -"
                
                # 🔐 Создаём Secret из .env (парсим на стороне Jenkins, передаём значения)
                DB_PASSWORD=\$(grep "^DB_PASSWORD=" \${ENV_FILE_PATH} | cut -d'=' -f2-)
                BOT_TOKEN=\$(grep "^BOT_TOKEN=" \${ENV_FILE_PATH} | cut -d'=' -f2-)
                ADMIN_PASSWORD=\$(grep "^ADMIN_PASSWORD=" \${ENV_FILE_PATH} | cut -d'=' -f2-)
                API_KEY=\$(grep "^API_KEY=" \${ENV_FILE_PATH} | cut -d'=' -f2-)
                
                run_k8s "kubectl create secret generic app-secrets \\
                    --from-literal=POSTGRES_DB_PASSWORD='${DB_PASSWORD}' \\
                    --from-literal=BOT_TOKEN='${BOT_TOKEN}' \\
                    --from-literal=ADMIN_PASSWORD='${ADMIN_PASSWORD}' \\
                    --from-literal=API_KEY='${API_KEY}' \\
                    -n \${NAMESPACE} \\
                    --dry-run=client -o yaml | kubectl apply -f -"
                
                # 📄 Создаём ConfigMap
                DB_NAME=\$(grep "^DB_NAME=" \${ENV_FILE_PATH} | cut -d'=' -f2-)
                DB_USERNAME=\$(grep "^DB_USERNAME=" \${ENV_FILE_PATH} | cut -d'=' -f2-)
                DB_HOST=\$(grep "^DB_HOST=" \${ENV_FILE_PATH} | cut -d'=' -f2-)
                DB_PORT=\$(grep "^DB_PORT=" \${ENV_FILE_PATH} | cut -d'=' -f2-)
                HTTP_PORT=\$(grep "^HTTP_PORT=" \${ENV_FILE_PATH} | cut -d'=' -f2-)
                HTTP_HOST=\$(grep "^HTTP_HOST=" \${ENV_FILE_PATH} | cut -d'=' -f2-)
                BOT_USERNAME=\$(grep "^BOT_USERNAME=" \${ENV_FILE_PATH} | cut -d'=' -f2-)
                
                run_k8s "kubectl create configmap app-config \\
                    --from-literal=DB_NAME='${DB_NAME}' \\
                    --from-literal=DB_USERNAME='${DB_USERNAME}' \\
                    --from-literal=DB_HOST='${DB_HOST}' \\
                    --from-literal=DB_PORT='${DB_PORT}' \\
                    --from-literal=HTTP_PORT='${HTTP_PORT}' \\
                    --from-literal=HTTP_HOST='${HTTP_HOST}' \\
                    --from-literal=BOT_USERNAME='${BOT_USERNAME}' \\
                    -n \${NAMESPACE} \\
                    --dry-run=client -o yaml | kubectl apply -f -"
                
                # 🗄️ Копируем манифесты на ВМ и применяем их
                echo "📦 Copying manifests to VM..."
                scp -i \${SSH_KEY_FILE} -o StrictHostKeyChecking=no \\
                    k8s/postgres.yml \\
                    ${SSH_USER}@${env.VM_IP}:/tmp/postgres.yml
                scp -i \${SSH_KEY_FILE} -o StrictHostKeyChecking=no \\
                    k8s/deployment.yml \\
                    ${SSH_USER}@${env.VM_IP}:/tmp/deployment.yml
                scp -i \${SSH_KEY_FILE} -o StrictHostKeyChecking=no \\
                    k8s/service.yml \\
                    ${SSH_USER}@${env.VM_IP}:/tmp/service.yml
                
                # 🗄️ Применяем PostgreSQL
                echo "🗄️ Deploying PostgreSQL..."
                run_k8s "kubectl apply -f /tmp/postgres.yml -n \${NAMESPACE}"
                
                # Ждём готовности БД
                echo "⏳ Waiting for PostgreSQL to be ready..."
                run_k8s "kubectl rollout status deployment/postgres -n \${NAMESPACE} --timeout=120s"
                
                # 🚀 Применяем приложение
                echo "🚀 Deploying application..."
                run_k8s "kubectl apply -f /tmp/deployment.yml -n \${NAMESPACE}"
                run_k8s "kubectl apply -f /tmp/service.yml -n \${NAMESPACE}"
                
                # Ждём готовности приложения
                run_k8s "kubectl rollout status deployment/movie-recommendation-bot -n \${NAMESPACE} --timeout=300s"
                
                # 🧹 Очистка временных файлов на ВМ
                run_k8s "rm -f /tmp/postgres.yml /tmp/deployment.yml /tmp/service.yml"
            """
                }
            }
        }

        stage('Health Check') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    withCredentials([
                            sshUserPrivateKey(
                                    credentialsId: 'ssh-private-key',
                                    keyFileVariable: 'SSH_KEY_FILE',
                                    usernameVariable: 'SSH_USER_VAR',
                                    passphraseVariable: ''
                            )
                    ]) {
                        sh """
                    echo "🔍 Running health checks on ${env.VM_IP}..."
                    
                    ssh -i \${SSH_KEY_FILE} \\
                        -o StrictHostKeyChecking=no \\
                        -o ConnectTimeout=10 \\
                        ${SSH_USER}@${env.VM_IP} "
                            echo '🔍 Checking pod status...'
                            kubectl get pods -l app=movie-bot -n ${K8S_NAMESPACE}
                            
                            echo '🔍 Checking service...'
                            kubectl get svc movie-bot-service -n ${K8S_NAMESPACE}
                            
                            # Показать NodePort для доступа
                            NODE_PORT=\$(kubectl get svc movie-bot-service -n ${K8S_NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo 'N/A')
                            if [ \"\$NODE_PORT\" != 'N/A' ]; then
                                echo \"✅ App available at: http://${env.VM_IP}:\$NODE_PORT\"
                            fi
                        "
                """
                    }
                }
            }
        }
    }

    post {
        success {
            echo "✅ Deployed to Kubernetes on VM!"
            echo "Namespace: ${K8S_NAMESPACE}"
            echo "App URL: http://${env.VM_IP}:30110 (если NodePort=30110)"
            echo "Check pods: ssh ${SSH_USER}@${env.VM_IP} 'kubectl get pods -l app=movie-bot -n ${K8S_NAMESPACE}'"
            echo "View logs: ssh ${SSH_USER}@${env.VM_IP} 'kubectl logs -l app=movie-bot -n ${K8S_NAMESPACE} -f'"
        }
        failure {
            echo "❌ Deployment failed!"
            echo "Debug commands:"
            echo "  ssh ${SSH_USER}@${env.VM_IP} 'kubectl describe deployment movie-recommendation-bot -n ${K8S_NAMESPACE}'"
            echo "  ssh ${SSH_USER}@${env.VM_IP} 'kubectl logs -l app=movie-bot -n ${K8S_NAMESPACE} --tail=50'"
            echo "  ssh ${SSH_USER}@${env.VM_IP} 'kubectl get events -n ${K8S_NAMESPACE} --sort-by=.metadata.creationTimestamp'"
        }
        always {
            cleanWs()
        }
    }
}