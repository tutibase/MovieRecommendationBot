pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "polyalugovenko/movie-recommendation-bot-new-infra"
        IMAGE_TAG = "latest"
        K8S_NAMESPACE = "movie-bot-ns"
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
                        file(credentialsId: 'kubeconfig-vm', variable: 'KUBECONFIG_FILE'),  // ← Changed to file
                        file(credentialsId: 'app-env-content', variable: 'ENV_FILE_PATH')
                ]) {
                    sh """
                # 🔧 1. Настраиваем PATH для kubectl
                export PATH=/var/jenkins_home:\${PATH}
                
                # 🔐 2. Используем файл kubeconfig напрямую
                export KUBECONFIG=\${KUBECONFIG_FILE}
                chmod 600 \${KUBECONFIG_FILE}
                
                # ✅ Проверка подключения (опционально, для отладки)
                echo "🔗 Testing connection..."
                kubectl cluster-info
                
                NAMESPACE="${K8S_NAMESPACE}"
                
                # Создаём namespace
                kubectl create namespace \${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
                
                # 🔐 Создаём Secret из .env
                kubectl create secret generic app-secrets \\
                    --from-literal=POSTGRES_DB_PASSWORD='\$(grep "^DB_PASSWORD=" \${ENV_FILE_PATH} | cut -d'=' -f2-)' \\
                    --from-literal=BOT_TOKEN='\$(grep "^BOT_TOKEN=" \${ENV_FILE_PATH} | cut -d'=' -f2-)' \\
                    --from-literal=ADMIN_PASSWORD='\$(grep "^ADMIN_PASSWORD=" \${ENV_FILE_PATH} | cut -d'=' -f2-)' \\
                    --from-literal=API_KEY='\$(grep "^API_KEY=" \${ENV_FILE_PATH} | cut -d'=' -f2-)' \\
                    -n \${NAMESPACE} \\
                    --dry-run=client -o yaml | kubectl apply -f -
                
                # 📄 Создаём ConfigMap
                kubectl create configmap app-config \\
                    --from-literal=DB_NAME='\$(grep "^DB_NAME=" \${ENV_FILE_PATH} | cut -d'=' -f2-)' \\
                    --from-literal=DB_USERNAME='\$(grep "^DB_USERNAME=" \${ENV_FILE_PATH} | cut -d'=' -f2-)' \\
                    --from-literal=DB_HOST='\$(grep "^DB_HOST=" \${ENV_FILE_PATH} | cut -d'=' -f2-)' \\
                    --from-literal=DB_PORT='\$(grep "^DB_PORT=" \${ENV_FILE_PATH} | cut -d'=' -f2-)' \\
                    --from-literal=HTTP_PORT='\$(grep "^HTTP_PORT=" \${ENV_FILE_PATH} | cut -d'=' -f2-)' \\
                    --from-literal=HTTP_HOST='\$(grep "^HTTP_HOST=" \${ENV_FILE_PATH} | cut -d'=' -f2-)' \\
                    --from-literal=BOT_USERNAME='\$(grep "^BOT_USERNAME=" \${ENV_FILE_PATH} | cut -d'=' -f2-)' \\
                    -n \${NAMESPACE} \\
                    --dry-run=client -o yaml | kubectl apply -f -
                
                # 🗄️ Применяем PostgreSQL
                echo "🗄️ Deploying PostgreSQL..."
                kubectl apply -f k8s/postgres.yml -n \${NAMESPACE}
                
                # Ждём готовности БД
                echo "⏳ Waiting for PostgreSQL to be ready..."
                kubectl rollout status deployment/postgres -n \${NAMESPACE} --timeout=120s
                
                # 🚀 Применяем приложение
                echo "🚀 Deploying application..."
                kubectl apply -f k8s/deployment.yml -n \${NAMESPACE}
                kubectl apply -f k8s/service.yml -n \${NAMESPACE}
                
                # Ждём готовности приложения
                kubectl rollout status deployment/movie-recommendation-bot -n \${NAMESPACE} --timeout=300s
            """
                }
            }
        }

        stage('Health Check') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    withCredentials([file(credentialsId: 'kubeconfig-vm', variable: 'KUBECONFIG_FILE')]) {
                        sh """
                    # 🔧 PATH и KUBECONFIG — в начале!
                    export PATH=/var/jenkins_home:\${PATH}
                    export KUBECONFIG=\${KUBECONFIG_FILE}
                    chmod 600 \${KUBECONFIG_FILE}
                    
                    echo "🔍 Checking pod status..."
                    kubectl get pods -l app=movie-bot -n ${K8S_NAMESPACE}
                    
                    echo "🔍 Checking service..."
                    kubectl get svc movie-bot-service -n ${K8S_NAMESPACE}
                    
                    # Показать NodePort для доступа
                    NODE_PORT=\$(kubectl get svc movie-bot-service -n ${K8S_NAMESPACE} -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "N/A")
                    if [ "\$NODE_PORT" != "N/A" ]; then
                        echo "✅ App available at: http://${env.VM_IP}:\${NODE_PORT}"
                    fi
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
            echo "Check pods: kubectl get pods -l app=movie-bot -n ${K8S_NAMESPACE}"
            echo "View logs: kubectl logs -l app=movie-bot -n ${K8S_NAMESPACE} -f"
        }
        failure {
            echo "❌ Deployment failed!"
            echo "Debug commands:"
            echo "  kubectl describe deployment movie-recommendation-bot -n ${K8S_NAMESPACE}"
            echo "  kubectl logs -l app=movie-bot -n ${K8S_NAMESPACE} --tail=50"
            echo "  kubectl get events -n ${K8S_NAMESPACE} --sort-by=.metadata.creationTimestamp"
        }
        always {
            cleanWs()
        }
    }
}