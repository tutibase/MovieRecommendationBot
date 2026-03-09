pipeline {
    agent { label 'poly-agent' }

    environment {
        // App config
        DOCKER_IMAGE = "polyalugovenko/movie-recommendation-bot"
        IMAGE_TAG = "latest"

        // Infra paths
        STACK_OUTPUTS = 'stack_outputs.json'
        SSH_KEY = "lugov-key-pair"
        APP_DIR = "/opt/movie-bot"

        COMPOSE_FILE = "MovieRecommendationBot/docker-compose.yml"
        INIT_SQL = "MovieRecommendationBot/src/main/resources/users_db.sql"
    }

    stages {
        stage('Get Infrastructure Info') {
            steps {
                // Копируем outputs из Лабы 3 (задача 'infra')
                copyArtifacts projectName: 'infra',
                        selector: lastSuccessful(),
                        filter: STACK_OUTPUTS,
                        target: '.',
                        flatten: true

                script {
                    // Парсим JSON и извлекаем IP
                    def outputs = readJSON file: STACK_OUTPUTS
                    env.VM_IP = outputs.server_private_ip?.output_value ?: ''

                    if (!env.VM_IP) {
                        error("Could not extract server_private_ip from ${STACK_OUTPUTS}")
                    }
                    echo "🌐 Target VM IP: ${env.VM_IP}"
                }
            }
        }

        stage('Copy Files to VM') {
            steps {
                sh '''
                    echo " Copying files to ${VM_IP}..."
                    
                    # Создаём директорию на ВМ
                    ssh -o StrictHostKeyChecking=no -i ~jenkins-poly/.ssh/${SSH_KEY} ubuntu@${VM_IP} "
                        mkdir -p ${APP_DIR}/init-db
                    "
                    
                    # Копируем docker-compose.yml
                    scp -i ~jenkins-poly/.ssh/${SSH_KEY} \
                        ${COMPOSE_FILE} \
                        ubuntu@${VM_IP}:${APP_DIR}/docker-compose.yml
                    
                    # Копируем скрипт инициализации БД
                    scp -i ~jenkins-poly/.ssh/${SSH_KEY} \
                        ${INIT_SQL} \
                        ubuntu@${VM_IP}:${APP_DIR}/init-db/users_db.sql
                    
                    echo "✅ Files copied successfully"
                '''
            }
        }

        stage('Deploy Application') {
            steps {
                withCredentials([
                        string(credentialsId: 'telegram-bot-token', variable: 'BOT_TOKEN'),
                        string(credentialsId: 'db-password', variable: 'DB_PASSWORD'),
                        string(credentialsId: 'admin-password', variable: 'ADMIN_PASSWORD'),
                        string(credentialsId: 'api-key', variable: 'API_KEY')
                ]) {
                    sh '''
                        echo " Deploying to ${VM_IP}..."
                        
                        ssh -o StrictHostKeyChecking=no -i ~jenkins-poly/.ssh/${SSH_KEY} ubuntu@${VM_IP} "
                            cd ${APP_DIR}
                            
                            # Генерируем .env файл с секретами
                            echo ' Creating .env file...'
                            cat > .env << EOF
                            DB_NAME=users_db
                            DB_USERNAME=users_db
                            DB_PASSWORD=${DB_PASSWORD}
                            BOT_TOKEN=${BOT_TOKEN}
                            BOT_USERNAME=MovieRecommendationBot
                            ADMIN_PASSWORD=${ADMIN_PASSWORD}
                            API_KEY=${API_KEY}
                            HTTP_PORT=8110
                            HTTP_HOST=0.0.0.0
                            EOF
                            
                            # Pull свежих образов
                            echo 'Pulling images...'
                            docker compose pull
                            
                            # Останавливаем старое (если есть)
                            echo 'Stopping old containers...'
                            docker compose down || true
                            
                            # Запускаем новое
                            echo 'Starting containers...'
                            docker compose up -d --force-recreate
                            
                            # Ждём запуска
                            echo ' Waiting for services...'
                            sleep 15
                            
                            # Проверка статуса
                            echo 'Checking status...'
                            docker compose ps
                        "
                    '''
                }
            }
        }

        stage('Health Check') {
            steps {
                timeout(time: 3, unit: 'MINUTES') {
                    sh '''
                        echo "🔍 Waiting for app to be healthy..."
                        ssh -i ~jenkins-poly/.ssh/${SSH_KEY} ubuntu@${VM_IP} "
                            timeout 180 bash -c '
                                echo \"  Checking containers...\"
                                while ! docker compose ps -q app | xargs -r docker inspect --format=\"{{.State.Status}}\" 2>/dev/null | grep -q running; do
                                    echo \"    Waiting for app container...\"
                                    sleep 5
                                done
                                echo \"  ✅ App container is running\"
                                
                                echo \"  Checking logs for errors...\"
                                if docker compose logs app --tail=100 2>&1 | grep -qiE \"connection refused|authentication failed|FATAL|Exception\"; then
                                    echo \"  ⚠️ Warning: potential errors in logs\"
                                    docker compose logs app --tail=30
                                    exit 1
                                else
                                    echo \"  ✅ No critical errors detected\"
                                fi
                                
                                echo \"  Checking DB connectivity...\"
                                if docker compose exec -T db pg_isready -U users_db -d users_db 2>/dev/null | grep -q \"accepting connections\"; then
                                    echo \"  ✅ Database is ready\"
                                else
                                    echo \"  ⚠️ Database may not be ready yet\"
                                fi
                            '
                        "
                    '''
                }
            }
        }
    }

    post {
        success {
            echo "✅ App deployed successfully!"
            echo "Telegram bot should be running"
            echo "App URL: http://${VM_IP}:8110 (from internal network)"
            echo " Logs: ssh ubuntu@${VM_IP} 'docker compose logs -f app'"
            echo " DB logs: ssh ubuntu@${VM_IP} 'docker compose logs -f db'"
        }
        failure {
            echo "❌ Deployment failed!"
            echo " Debug commands:"
            echo "  ssh ubuntu@${VM_IP} 'docker compose ps'"
            echo "  ssh ubuntu@${VM_IP} 'docker compose logs app --tail=50'"
            echo "  ssh ubuntu@${VM_IP} 'docker compose logs db --tail=50'"
        }
        always {
            cleanWs()
        }
    }
}