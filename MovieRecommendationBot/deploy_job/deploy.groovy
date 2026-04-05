pipeline {
    agent any

    environment {
        // App config
        DOCKER_IMAGE = "polyalugovenko/movie-recommendation-bot-new-infra"
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
                            
                            # 🔍 Простая и надёжная проверка: ищем контейнер по имени сервиса в docker ps
                            for i in {1..30}; do  # 30 попыток * 10 сек = 5 минут
                                # Проверяем, есть ли контейнер со статусом "Up" в выводе
                                if docker ps --format "{{.Names}}|{{.Status}}" | grep -E "movie-bot-poly.*Up"; then
                                    echo "  ✅ App container is running"
                                    break
                                fi
                                
                                echo "    Waiting for app container... ($i/30)"
                                sleep 10
                            done
                            
                            # Финальная проверка: если контейнер не найден — ошибка
                            if ! docker ps --format "{{.Names}}" | grep -q "movie-bot-poly"; then
                                echo "  ❌ App container not found!"
                                echo "  === All containers ==="
                                docker ps -a
                                exit 1
                            fi
                            
                            echo "  Checking logs for critical errors..."
                            if docker compose logs app --tail=50 2>&1 | grep -qiE "fatal|exception|authentication failed|error"; then
                                echo "  ⚠️ Warning: potential errors in logs"
                                docker compose logs app --tail=20 || true
                                # Не завершаем с ошибкой, если приложение всё ещё работает
                            else
                                echo "  ✅ No critical errors detected in recent logs"
                            fi
                            
                            echo "  Checking DB connectivity..."
                            if docker compose exec -T db pg_isready -U users_db -d users_db 2>/dev/null | grep -q "accepting"; then
                                echo "  ✅ Database is ready and accepting connections"
                            else
                                echo "  ⚠️ Database may not be ready yet, but continuing..."
                            fi
                            
                            echo "  Final status check:"
                            docker compose ps
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