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
                copyArtifacts projectName: 'infra',
                        selector: lastSuccessful(),
                        filter: STACK_OUTPUTS,
                        target: '.',
                        flatten: true

                script {
                    def outputs = readJSON file: STACK_OUTPUTS
                    def privateIpJson = readJSON text: outputs.server_private_ip
                    env.VM_IP = privateIpJson.output_value

                    if (!env.VM_IP) {
                        error("❌ Could not extract server_private_ip from ${STACK_OUTPUTS}")
                    }
                    echo "🌐 Target VM IP: ${env.VM_IP}"
                }
            }
        }

        stage('Copy Files to VM') {
            steps {
                sshagent([SSH_KEY]) {
                    sh """
                echo "📁 Copying files to ${env.VM_IP}..."
                
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
                withCredentials([
                        string(credentialsId: 'telegram-bot-token', variable: 'BOT_TOKEN'),
                        string(credentialsId: 'POSTGRES_DB_PASSWORD', variable: 'POSTGRES_DB_PASSWORD'),
                        string(credentialsId: 'admin-password', variable: 'ADMIN_PASSWORD'),
                        string(credentialsId: 'api-key', variable: 'API_KEY')
                ]) {
                    script {
                        // 🔐 Минимальная отладка пароля
                        echo "🔐 POSTGRES_DB_PASSWORD: length=${POSTGRES_DB_PASSWORD?.length() ?: 0}"
                    }
                    sshagent([SSH_KEY]) {
                        sh """
                    echo "Deploying to ${env.VM_IP}..."
                    
                    ssh -o StrictHostKeyChecking=no ubuntu@${env.VM_IP} "
                        cd ${APP_DIR}
                        
                        echo 'Creating .env file...'
                        printf '%s\\n' \\
                            'DB_NAME=users_db' \\
                            'DB_USERNAME=users_db' \\
                            'POSTGRES_DB_PASSWORD=${POSTGRES_DB_PASSWORD}' \\
                            'DB_URL=jdbc:postgresql://db:5432/users_db' \\
                            'BOT_TOKEN=${BOT_TOKEN}' \\
                            'BOT_USERNAME=Poly_MovieRecommendationBot' \\
                            'ADMIN_PASSWORD=${ADMIN_PASSWORD}' \\
                            'API_KEY=${API_KEY}' \\
                            'HTTP_PORT=8110' \\
                            'HTTP_HOST=0.0.0.0' \\
                            > .env
                        
                        # 🔍 Проверка, что .env создался
                        echo 'Verifying .env...'
                        grep -c 'POSTGRES_DB_PASSWORD' .env && echo '✅ POSTGRES_DB_PASSWORD in .env'
                        
                        echo 'Pulling images...'
                        docker compose pull
                        
                        echo 'Stopping old containers...'
                        docker compose down || true
                        
                        echo 'Starting containers...'
                        docker compose up -d --force-recreate
                        
                        echo 'Waiting for services...'
                        sleep 15
                        
                        echo 'Checking status...'
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