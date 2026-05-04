pipeline {
    agent { label 'agent' }

    environment {
        // App config
        DOCKER_IMAGE = "polyalugovenko/movie-recommendation-bot"
        IMAGE_TAG = "latest"
        PATH="/home/ubuntu/venv/bin:$PATH"

        // Infra paths
        STACK_OUTPUTS = 'stack_outputs.json'
        SSH_KEY = "ubuntu-key"
        APP_DIR = "/opt/movie-bot"

        COMPOSE_FILE = "MovieRecommendationBot/docker-compose.yml"
        INIT_SQL = "MovieRecommendationBot/src/main/resources/users_db.sql"
    }

    stages {
        stage('Get Infrastructure Info') {
            steps {
                copyArtifacts projectName: 'infra_heat',
                        selector: lastSuccessful(),
                        filter: STACK_OUTPUTS,
                        target: '.',
                        flatten: true

                script {
                    def rawContent = readFile file: STACK_OUTPUTS, encoding: 'UTF-8'
                    def outputs = readJSON text: rawContent
                    def ipObj = outputs.server_private_ip
                    def ipVal = ipObj.get('output_value')
                    if (ipVal) {
                        String cleanIp = ipVal.toString().trim()

                        env.VM_IP = cleanIp

                        echo "env.VM_IP set to string: [${env.VM_IP}]"
                    } else {
                        error("Failed to extract IP.")
                    }
                }
            }
        }

        stage('Copy Files to VM') {
            steps {
                withCredentials([sshUserPrivateKey(
                        credentialsId: 'ubuntu-key',
                        keyFileVariable: 'SSH_KEY_FILE',
                        usernameVariable: 'SSH_USER'
                )]) {
                    sh """
                        echo "Copying files to ${env.VM_IP}..."
                        
                        # Создаем директорию
                        ssh -i \${SSH_KEY_FILE} -o StrictHostKeyChecking=no \${SSH_USER}@${env.VM_IP} "mkdir -p /opt/movie-bot/init-db"
                        
                        # Копируем docker-compose.yml
                        scp -i \${SSH_KEY_FILE} -o StrictHostKeyChecking=no \\
                            MovieRecommendationBot/docker-compose.yml \\
                            \${SSH_USER}@${env.VM_IP}:/opt/movie-bot/
                        
                        # Копируем SQL файл
                        scp -i \${SSH_KEY_FILE} -o StrictHostKeyChecking=no \\
                            MovieRecommendationBot/src/main/resources/users_db.sql \\
                            \${SSH_USER}@${env.VM_IP}:/opt/movie-bot/init-db/
                        
                        echo "Files copied"
                    """
                }
            }
        }

        stage('Deploy Application') {
            steps {
                script {
                    def deployUser = env.SSH_USER ?: 'ubuntu'
                    def deployHost = env.VM_IP

                    withCredentials([
                            file(credentialsId: 'app-env-content', variable: 'APP_ENV_FILE'),
                            sshUserPrivateKey(
                                    credentialsId: 'ubuntu-key',
                                    keyFileVariable: 'SSH_KEY_FILE',
                                    usernameVariable: 'SSH_USER',
                                    passphraseVariable: ''
                            )
                    ]) {
                        sh """
                    echo "Deploying to ${deployHost}..."
                    
                    ssh -i \${SSH_KEY_FILE} \\
                        -o StrictHostKeyChecking=no \\
                        -o ConnectTimeout=10 \\
                        ${deployUser}@${deployHost} "
                            rm -f ${APP_DIR}/.env
                        "
                    
                    # Копируем новый .env файл
                    echo "Copying .env file..."
                    scp -i \${SSH_KEY_FILE} \\
                        -o StrictHostKeyChecking=no \\
                        -o ConnectTimeout=30 \\
                        \"\${APP_ENV_FILE}\" \\
                        ${deployUser}@${deployHost}:${APP_DIR}/.env
                    
                    # Копируем docker-compose.yml
                    if [ -f "${COMPOSE_FILE}" ]; then
                        scp -i \${SSH_KEY_FILE} \\
                            -o StrictHostKeyChecking=no \\
                            -o ConnectTimeout=30 \\
                            ${COMPOSE_FILE} \\
                            ${deployUser}@${deployHost}:${APP_DIR}/
                    fi
                    
                    # Запускаем приложение
                    echo "Starting application..."
                    ssh -i \${SSH_KEY_FILE} \\
                        -o StrictHostKeyChecking=no \\
                        -o ConnectTimeout=10 \\
                        ${deployUser}@${deployHost} "
                            cd ${APP_DIR}
                            
                            if [ -f .env ]; then
                                echo '.env file exists'
                            else
                                echo 'ERROR: .env file not found!'
                                exit 1
                            fi
                            
                            docker compose pull
                            docker compose down || true
                            docker compose up -d --force-recreate
                            sleep 15
                            docker compose ps
                        "
                """
                    }
                }
            }
        }
    }

    post {
        success {
            echo "✅ App deployed successfully!"
            echo "Telegram bot should be running"
            echo "App URL: http://${env.VM_IP}:8110 (from internal network)"
            echo " Logs: ssh ubuntu@${env.VM_IP} 'docker compose logs -f app'"
            echo " DB logs: ssh ubuntu@${env.VM_IP} 'docker compose logs -f db'"
        }
        failure {
            echo "❌ Deployment failed!"
            echo " Debug commands:"
            echo "  ssh ubuntu@${env.VM_IP} 'docker compose ps'"
            echo "  ssh ubuntu@${env.VM_IP} 'docker compose logs app --tail=50'"
            echo "  ssh ubuntu@${env.VM_IP} 'docker compose logs db --tail=50'"
        }
        always {
            cleanWs()
        }
    }
}