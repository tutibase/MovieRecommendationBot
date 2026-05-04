pipeline {
    agent { label 'agent' }

    environment {
        // App config
        DOCKER_IMAGE = "polyalugovenko/movie-recommendation-bot"
        IMAGE_TAG = "latest"
        PATH="/home/ubuntu/venv/bin:$PATH"
        VM_IP = ""

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
                copyArtifacts projectName: 'infra_heat',
                        selector: lastSuccessful(),
                        filter: STACK_OUTPUTS,
                        target: '.',
                        flatten: true

                script {
                    // Читаем и парсим JSON
                    def outputs = readJSON file: STACK_OUTPUTS

                    // Явно получаем объект сервера
                    def serverIpObj = outputs['server_private_ip']

                    echo "DEBUG: Type of server_private_ip: ${serverIpObj.getClass().getName()}"
                    echo "DEBUG: Content of server_private_ip: ${serverIpObj}"

                    if (serverIpObj instanceof java.util.Map) {
                        // Если это карта, берем output_value
                        env.VM_IP = serverIpObj['output_value']
                    } else if (serverIpObj instanceof String) {
                        // Если вдруг это уже строка (на всякий случай)
                        env.VM_IP = serverIpObj
                    } else {
                        error("Unexpected type for server_private_ip: ${serverIpObj.getClass().getName()}")
                    }

                    echo "DEBUG: Extracted VM_IP: '${env.VM_IP}'"

                    if (!env.VM_IP || env.VM_IP.trim().isEmpty()) {
                        error("Could not extract server_private_ip from ${STACK_OUTPUTS}. Value is empty.")
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
                
                ssh -o StrictHostKeyChecking=no ubuntu@${env.VM_IP} "
                    mkdir -p /opt/movie-bot/init-db
                "
                
                scp -o StrictHostKeyChecking=no \\
                    MovieRecommendationBot/docker-compose.yml \\
                    ubuntu@${env.VM_IP}:/opt/movie-bot/
                
                scp -o StrictHostKeyChecking=no \\
                    MovieRecommendationBot/src/main/resources/users_db.sql \\
                    ubuntu@${env.VM_IP}:/opt/movie-bot/init-db/
                
                echo "✅ Files copied"
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
                                    credentialsId: 'ssh-private-key',
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
                                echo '✅ .env file exists'
                            else
                                echo '❌ ERROR: .env file not found!'
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

        stage('Health Check') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    sshagent([SSH_KEY]) {
                        sh '''
                    echo "Waiting for app to be healthy..."
                    
                    ssh -o StrictHostKeyChecking=no ubuntu@${VM_IP} '
                        set +e 
                        
                        echo "  Checking containers..."
                        CONTAINER_NAME="movie-bot-poly"
                        
                        for i in {1..60}; do 
                            STATUS=$(docker compose ps -q app 2>/dev/null | head -1 | xargs -r docker inspect --format="{{.State.Status}}" 2>/dev/null | tr -d "[:space:]")
                            
                            if [ "$STATUS" = "running" ]; then
                                echo "  ✅ App container is running"
                                break
                            fi
                           
                            echo "    Waiting for app container... ($i/60) - status: ${STATUS:-unknown}"
                            sleep 5
                        done
                        
                        if [ "$STATUS" != "running" ]; then
                            echo "  ❌ App container failed to start (status: $STATUS)"
                            echo "  === Container status ==="
                            docker compose ps
                            echo "  === Last 30 app logs ==="
                            docker compose logs app --tail=30 || true
                            exit 1
                        fi
                        
                        echo "  Checking logs for critical errors..."
                        if docker compose logs app --tail=100 2>&1 | grep -qiE "fatal|exception|authentication failed"; then
                            echo "  ⚠️ Warning: critical errors in logs"
                            docker compose logs app --tail=30 || true
                            exit 1
                        else
                            echo "  ✅ No critical errors detected"
                        fi
                        
                        echo "  Checking DB..."
                        if docker compose exec -T db pg_isready -U users_db -d users_db 2>/dev/null | grep -q "accepting"; then
                            echo "  ✅ Database is ready"
                        else
                            echo "  ⚠️ Database may not be ready yet"
                        fi
                    '
                '''
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