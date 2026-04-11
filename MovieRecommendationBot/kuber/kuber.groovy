pipeline {
    agent any

    environment {
        PG_DIR = 'MovieRecommendationBot/kuber/k8s/postgres.yml'
        DEPLOY_DIR = 'MovieRecommendationBot/kuber/k8s/deployment.yml'
        SERVICE_DIR = 'MovieRecommendationBot/kuber/k8s/service.yml'
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
                                credentialsId: 'ssh-private-key',
                                keyFileVariable: 'SSH_KEY_FILE',
                                usernameVariable: 'SSH_USER_VAR',
                                passphraseVariable: ''
                        ),
                        file(credentialsId: 'app-env-content', variable: 'ENV_FILE_PATH')
                ]) {
                    sh """
                echo "🔗 Connecting to VM ${env.VM_IP}..."
                
                # 🔐 Парсим переменные из .env (с tr -d '\r' для Windows line endings)
                DB_PASSWORD=\$(grep "^DB_PASSWORD=" \${ENV_FILE_PATH} | cut -d'=' -f2- | tr -d '\r')
                BOT_TOKEN=\$(grep "^BOT_TOKEN=" \${ENV_FILE_PATH} | cut -d'=' -f2- | tr -d '\r')
                ADMIN_PASSWORD=\$(grep "^ADMIN_PASSWORD=" \${ENV_FILE_PATH} | cut -d'=' -f2- | tr -d '\r')
                API_KEY=\$(grep "^API_KEY=" \${ENV_FILE_PATH} | cut -d'=' -f2- | tr -d '\r')
                DB_NAME=\$(grep "^DB_NAME=" \${ENV_FILE_PATH} | cut -d'=' -f2- | tr -d '\r')
                DB_USERNAME=\$(grep "^DB_USERNAME=" \${ENV_FILE_PATH} | cut -d'=' -f2- | tr -d '\r')
                DB_HOST=\$(grep "^DB_HOST=" \${ENV_FILE_PATH} | cut -d'=' -f2- | tr -d '\r')
                DB_PORT=\$(grep "^DB_PORT=" \${ENV_FILE_PATH} | cut -d'=' -f2- | tr -d '\r')
                HTTP_PORT=\$(grep "^HTTP_PORT=" \${ENV_FILE_PATH} | cut -d'=' -f2- | tr -d '\r')
                HTTP_HOST=\$(grep "^HTTP_HOST=" \${ENV_FILE_PATH} | cut -d'=' -f2- | tr -d '\r')
                BOT_USERNAME=\$(grep "^BOT_USERNAME=" \${ENV_FILE_PATH} | cut -d'=' -f2- | tr -d '\r')
                
                # ✅ Формируем DB_URL из компонентов
                DB_URL="jdbc:postgresql://\${DB_HOST}:\${DB_PORT}/\${DB_NAME}"
                
                # 🗄️ Копируем манифесты на ВМ (с проверкой)
                echo "📦 Copying manifests to VM..."
                for f in ${PG_DIR} ${DEPLOY_DIR} ${SERVICE_DIR}; do
                    if [ ! -f "\$f" ]; then
                        echo "❌ Source file not found: \$f"
                        exit 1
                    fi
                done
                
                scp -i \${SSH_KEY_FILE} -o StrictHostKeyChecking=no -o ConnectTimeout=30 \\
                    ${PG_DIR} ${SSH_USER}@${env.VM_IP}:/tmp/postgres.yml || { echo "❌ scp failed"; exit 1; }
                scp -i \${SSH_KEY_FILE} -o StrictHostKeyChecking=no -o ConnectTimeout=30 \\
                    ${DEPLOY_DIR} ${SSH_USER}@${env.VM_IP}:/tmp/deployment.yml || { echo "❌ scp failed"; exit 1; }
                scp -i \${SSH_KEY_FILE} -o StrictHostKeyChecking=no -o ConnectTimeout=30 \\
                    ${SERVICE_DIR} ${SSH_USER}@${env.VM_IP}:/tmp/service.yml || { echo "❌ scp failed"; exit 1; }
                
                # 🗄️ Копируем SQL скрипт инициализации
                echo "📦 Copying SQL init script..."
                scp -i \${SSH_KEY_FILE} -o StrictHostKeyChecking=no -o ConnectTimeout=30 \\
                    MovieRecommendationBot/src/main/resources/users_db.sql \\
                    ${SSH_USER}@${env.VM_IP}:/tmp/users_db.sql || echo "⚠️ SQL script copy failed (optional)"
                
                echo "✅ Manifests copied"
                
                # 🔧 ОДНО SSH-подключение для всех kubectl-команд
                ssh -i \${SSH_KEY_FILE} \\
                    -o StrictHostKeyChecking=no \\
                    -o ConnectTimeout=30 \\
                    -o ServerAliveInterval=30 \\
                    -o ServerAliveCountMax=3 \\
                    ${SSH_USER}@${env.VM_IP} "
                        set -e
                        echo '🔗 Testing connection...'
                        kubectl cluster-info
                        
                        echo '📦 Creating namespace...'
                        kubectl create namespace ${K8S_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
                        
                        echo '🔐 Creating Secret...'
                        kubectl create secret generic app-secrets \\
                            --from-literal=DB_PASSWORD='\${DB_PASSWORD}' \\
                            --from-literal=BOT_TOKEN='\${BOT_TOKEN}' \\
                            --from-literal=ADMIN_PASSWORD='\${ADMIN_PASSWORD}' \\
                            --from-literal=API_KEY='\${API_KEY}' \\
                            -n ${K8S_NAMESPACE} \\
                            --dry-run=client -o yaml | kubectl apply -f -
                        
                        echo '📄 Creating ConfigMap...'
                        kubectl create configmap app-config \\
                            --from-literal=DB_URL="\${DB_URL}" \\
                            --from-literal=DB_NAME='\${DB_NAME}' \\
                            --from-literal=DB_USERNAME='\${DB_USERNAME}' \\
                            --from-literal=DB_HOST='\${DB_HOST}' \\
                            --from-literal=DB_PORT='\${DB_PORT}' \\
                            --from-literal=HTTP_PORT='\${HTTP_PORT}' \\
                            --from-literal=HTTP_HOST='\${HTTP_HOST}' \\
                            --from-literal=BOT_USERNAME='\${BOT_USERNAME}' \\
                            -n ${K8S_NAMESPACE} \\
                            --dry-run=client -o yaml | kubectl apply -f -
                        
                        echo '🗄️ Deploying PostgreSQL...'
                        kubectl apply -f /tmp/postgres.yml -n ${K8S_NAMESPACE}
                        
                        echo '⏳ Waiting for PostgreSQL...'
                        kubectl rollout status deployment/postgres -n ${K8S_NAMESPACE} --timeout=120s
                        
                        
                        echo '🚀 Deploying application...'
                        kubectl apply -f /tmp/deployment.yml -n ${K8S_NAMESPACE}
                        kubectl apply -f /tmp/service.yml -n ${K8S_NAMESPACE}
                        
                        echo '⏳ Waiting for application...'
                        kubectl rollout status deployment/movie-recommendation-bot -n ${K8S_NAMESPACE} --timeout=300s
                        
                        echo '🧹 Cleaning up...'
                        rm -f /tmp/postgres.yml /tmp/deployment.yml /tmp/service.yml
                    "
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