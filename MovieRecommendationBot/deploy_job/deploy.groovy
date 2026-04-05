pipeline {
    agent any

    environment {
        // App config
        DOCKER_IMAGE = "polyalugovenko/movie-recommendation-bot-new"
        IMAGE_TAG = "latest"

        // Infra paths
        APP_DIR = "/opt/movie-bot"
        COMPOSE_FILE = "MovieRecommendationBot/docker-compose.yml"
        INIT_SQL = "MovieRecommendationBot/src/main/resources/users_db.sql"

        INFRA_JOB_NAME = 'cloud_infra'
        STACK_OUTPUTS = 'terraform_outputs.json'  // или имя файла с выводом Terraform
    }

    options {
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Get Infrastructure Info') {
            steps {
                script {
                    // Копируем файл с IP из джобы cloud_infra
                    copyArtifacts(
                            projectName: 'cloud_infra',
                            selector: lastSuccessful(),
                            filter: 'MovieRecommendationBot/outputs/vm_ip.txt',  // ← ✅ Добавлен префикс!
                            target: '.',
                            flatten: true  // Благодаря этому файл появится как просто 'vm_ip.txt'
                    )

                    // Читаем IP из файла
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

        stage('Copy Files to VM') {
            steps {
                withCredentials([sshUserPrivateKey(
                        credentialsId: 'ssh-private-key',
                        keyFileVariable: 'SSH_KEY_FILE',
                        usernameVariable: 'SSH_USER',
                        passphraseVariable: ''
                )]) {
                    sh """
                        echo "📁 Copying files to ${env.VM_IP}..."
                        
                        # Создаём директорию на ВМ
                        ssh -i \${SSH_KEY_FILE} \\
                            -o StrictHostKeyChecking=no \\
                            -o ConnectTimeout=10 \\
                            \${SSH_USER:-ubuntu}@${env.VM_IP} "
                                mkdir -p ${APP_DIR}/init-db
                            "
                        
                        # Копируем docker-compose.yml
                        scp -i \${SSH_KEY_FILE} \\
                            -o StrictHostKeyChecking=no \\
                            -o ConnectTimeout=30 \\
                            ${COMPOSE_FILE} \\
                            \${SSH_USER:-ubuntu}@${env.VM_IP}:${APP_DIR}/
                        
                        # Копируем init SQL (если есть)
                        if [ -f "${INIT_SQL}" ]; then
                            scp -i \${SSH_KEY_FILE} \\
                                -o StrictHostKeyChecking=no \\
                                -o ConnectTimeout=30 \\
                                ${INIT_SQL} \\
                                \${SSH_USER:-ubuntu}@${env.VM_IP}:${APP_DIR}/init-db/
                            echo "✅ SQL init file copied"
                        fi
                        
                        echo "✅ Files copied"
                    """
                }
            }
        }

        stage('Deploy Application') {
            steps {
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
                echo "Deploying to ${env.VM_IP}..."
                
                # 1. Гарантированно создаём директорию на ВМ
                ssh -i \${SSH_KEY_FILE} \\
                    -o StrictHostKeyChecking=no \\
                    -o ConnectTimeout=10 \\
                    \${SSH_USER:-ubuntu}@${env.VM_IP} "
                        mkdir -p ${APP_DIR}
                    "
                
                # 2. Копируем готовый .env файл напрямую через scp
                echo "Copying .env file..."
                scp -i \${SSH_KEY_FILE} \\
                    -o StrictHostKeyChecking=no \\
                    -o ConnectTimeout=30 \\
                    \"${APP_ENV_FILE}\" \\
                    \${SSH_USER:-ubuntu}@${env.VM_IP}:${APP_DIR}/.env
                
                # 3. Копируем docker-compose.yml (если ещё не скопирован)
                if [ -f "${COMPOSE_FILE}" ]; then
                    scp -i \${SSH_KEY_FILE} \\
                        -o StrictHostKeyChecking=no \\
                        -o ConnectTimeout=30 \\
                        ${COMPOSE_FILE} \\
                        \${SSH_USER:-ubuntu}@${env.VM_IP}:${APP_DIR}/
                fi
                
                # 4. Запускаем приложение на ВМ
                echo "Starting application..."
                ssh -i \${SSH_KEY_FILE} \\
                    -o StrictHostKeyChecking=no \\
                    -o ConnectTimeout=10 \\
                    \${SSH_USER:-ubuntu}@${env.VM_IP} "
                        cd ${APP_DIR}
                        
                        # Проверка, что .env скопировался
                        if [ -f .env ]; then
                            echo '✅ .env file exists'
                            # Покажем только названия переменных (без значений!) для отладки
                            echo '🔍 Variables in .env:'
                            cut -d'=' -f1 .env | head -10
                        else
                            echo '❌ ERROR: .env file not found!'
                            exit 1
                        fi
                        
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

        stage('Health Check') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    withCredentials([sshUserPrivateKey(
                            credentialsId: 'ssh-private-key',
                            keyFileVariable: 'SSH_KEY_FILE',
                            usernameVariable: 'SSH_USER',
                            passphraseVariable: ''
                    )]) {
                        sh '''
                            echo "Waiting for app to be healthy..."
                            
                            ssh -i ${SSH_KEY_FILE} \\
                                -o StrictHostKeyChecking=no \\
                                -o ConnectTimeout=10 \\
                                ${SSH_USER:-ubuntu}@${VM_IP} '
                                    set +e 
                                    
                                    echo "  Checking containers..."
                                    
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
            echo "App URL: http://${env.VM_IP}:8110"
            echo "Logs: ssh -i <key> ubuntu@${env.VM_IP} 'docker compose logs -f app'"
        }
        failure {
            echo "❌ Deployment failed!"
            echo "Debug commands:"
            echo "  ssh -i <key> ubuntu@${env.VM_IP} 'docker compose ps'"
            echo "  ssh -i <key> ubuntu@${env.VM_IP} 'docker compose logs app --tail=50'"
            echo "  ssh -i <key> ubuntu@${env.VM_IP} 'docker compose logs db --tail=50'"
        }
        always {
            cleanWs()
        }
    }
}