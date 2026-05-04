pipeline {
    agent { label 'agent' }

    environment {
        // Инициализируем пустым значением, чтобы переменная существовала в контексте
        VM_IP = ""

        DOCKER_IMAGE = "polyalugovenko/movie-recommendation-bot"
        IMAGE_TAG = "latest"

        STACK_OUTPUTS = 'stack_outputs.json'
        SSH_KEY = "lugov-key-pair"
        APP_DIR = "/opt/movie-bot"
        COMPOSE_FILE = "MovieRecommendationBot/docker-compose.yml"
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
                    // 1. Читаем основной файл артефакта
                    def outputs = readJSON file: STACK_OUTPUTS

                    echo "DEBUG: Full outputs structure: ${outputs}"
                    echo "DEBUG: Type of server_private_ip: ${outputs.server_private_ip.getClass().getName()}"

                    def ipValue

                    // 2. Проверяем тип данных поля server_private_ip
                    if (outputs.server_private_ip instanceof String) {
                        // Если это строка, значит там экранированный JSON, нужно парсить еще раз
                        echo "DEBUG: Parsing nested JSON string..."
                        def parsed = readJSON text: outputs.server_private_ip
                        ipValue = parsed.output_value
                    } else {
                        // Если это объект/Map (как в вашей ошибке), берем поле напрямую
                        echo "DEBUG: Taking value directly from object..."
                        ipValue = outputs.server_private_ip.output_value
                    }

                    env.VM_IP = ipValue ? ipValue.toString() : ""

                    if (!env.VM_IP) {
                        error("Could not extract IP. Value is null or empty.")
                    }

                    echo "Target VM IP: ${env.VM_IP}"
                }
            }
        }

        stage('Copy Files to VM') {
            steps {
                sshagent([SSH_KEY]) {
                    sh """
                        echo "Copying files to ${env.VM_IP}..."
                        ssh -o StrictHostKeyChecking=no ubuntu@${env.VM_IP} "mkdir -p /opt/movie-bot/init-db"
                        
                        scp -o StrictHostKeyChecking=no \\
                            MovieRecommendationBot/docker-compose.yml \\
                            ubuntu@${env.VM_IP}:/opt/movie-bot/
                        
                        scp -o StrictHostKeyChecking=no \\
                            MovieRecommendationBot/src/main/resources/users_db.sql \\
                            ubuntu@${env.VM_IP}:/opt/movie-bot/init-db/
                    """
                }
            }
        }

        stage('Deploy Application') {
            steps {
                script {
                    withCredentials([
                            file(credentialsId: 'app-env-content', variable: 'APP_ENV_FILE'),
                            sshUserPrivateKey(
                                    credentialsId: 'ssh-private-key',
                                    keyFileVariable: 'SSH_KEY_FILE',
                                    usernameVariable: 'SSH_USER'
                            )
                    ]) {
                        sh """
                            # Удаляем старый .env
                            ssh -i \${SSH_KEY_FILE} -o StrictHostKeyChecking=no ubuntu@${env.VM_IP} "rm -f ${APP_DIR}/.env"
                            
                            # Копируем новый .env и compose
                            scp -i \${SSH_KEY_FILE} -o StrictHostKeyChecking=no "\${APP_ENV_FILE}" ubuntu@${env.VM_IP}:${APP_DIR}/.env
                            scp -i \${SSH_KEY_FILE} -o StrictHostKeyChecking=no ${COMPOSE_FILE} ubuntu@${env.VM_IP}:${APP_DIR}/
                            
                            # Деплой
                            ssh -i \${SSH_KEY_FILE} -o StrictHostKeyChecking=no ubuntu@${env.VM_IP} "
                                cd ${APP_DIR}
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

        // ... Stage Health Check остается без изменений ...
    }

    post {
        success {
            echo "✅ Deployed successfully!"
        }
        failure {
            echo "❌ Deployment failed!"

            // БЕЗОПАСНОЕ обращение к переменной в post-блоке
            // Используем ?. (safe navigation) и toString(), чтобы избежать MissingPropertyException
            def currentIp = env.VM_IP?.toString()

            if (currentIp && !currentIp.isEmpty()) {
                echo "Debug commands for ${currentIp}:"
                echo "  ssh ubuntu@${currentIp} 'docker compose ps'"
                echo "  ssh ubuntu@${currentIp} 'docker compose logs --tail=50'"
            } else {
                echo "⚠️ VM_IP was not set. Check 'Get Infrastructure Info' stage logs."
                echo "Possible cause: stack_outputs.json format mismatch or copyArtifacts failure."
            }
        }
        always {
            cleanWs()
        }
    }
}