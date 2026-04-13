pipeline {
    agent any

    environment {
        PROJECT_DIR = 'MovieRecommendationBot'
        K8S_DIR = 'MovieRecommendationBot/k8s'
        IMAGE_NAME = 'movie-bot:latest'
        NAMESPACE = 'default'
    }

    stages {
        // ==========================================
        // ЭТАП 1: СБОРКА И DOCKER ОБРАЗ
        // ==========================================
        stage('Build & Dockerize') {
            steps {
                script {
                    echo '🔨 Building Application...'

                    // 1. Временная БД для сборки (jOOQ требует БД для генерации кода)
                    sh """
                        docker run -d --name build-db -e POSTGRES_PASSWORD=password -e POSTGRES_DB=users_db -p 54321:5432 postgres:15
                        sleep 15
                        cat ${PROJECT_DIR}/src/main/resources/users_db.sql | docker exec -i build-db psql -U postgres -d users_db || true
                    """

                    // 2. Сборка JAR через Maven (здесь jOOQ сработает, так как есть БД)
                    dir("${PROJECT_DIR}") {
                        sh """
                            mvn clean package -DskipTests \\
                                -Ddb.password=password \\
                                -Ddb.url=jdbc:postgresql://host.docker.internal:54321/users_db \\
                                -Ddb.user=postgres
                        """
                    }

                    sh 'docker rm -f build-db || true'

                    // 3. Сборка Docker образа
                    // Теперь Dockerfile простой, он просто копирует готовый JAR
                    echo '🐳 Building Docker Image...'

                    // Важно: собираем образ из папки PROJECT_DIR, где лежит target/ и новый Dockerfile
                    // Если Dockerfile лежит в корне PROJECT_DIR, то команда верная.
                    sh "docker build -t ${IMAGE_NAME} ${PROJECT_DIR}/"
                }
            }
        }

        // ==========================================
        // ЭТАП 2: DEPLOY TO KUBERNETES
        // ==========================================
        stage('Deploy to K8s') {
            steps {
                script {
                    echo '📄 Checking K8s Connection...'
                    sh "kubectl cluster-info"

                    withCredentials([file(credentialsId: 'bot-env-file', variable: 'ENV_FILE_PATH')]) {

                        echo '📄 Parsing .env and creating K8s resources...'

                        // Парсим .env файл в переменные окружения bash
                        // Используем export, чтобы переменные были доступны следующим командам
                        sh """
                            # Читаем файл построчно, игнорируем комментарии и пустые строки
                            while IFS='=' read -r key value; do
                                # Пропускаем комментарии и пустые строки
                                case "\$key" in
                                    \\#*|"") continue ;;
                                esac
                                # Удаляем возможные кавычки и пробелы
                                value=\$(echo "\$value" | sed 's/^"\\(.*\\)"\$/\\1/' | sed "s/^'\\(.*\\)'\$/\\1/")
                                export "\$key=\$value"
                            done < \${ENV_FILE_PATH}

                            # Проверяем, что ключевые переменные загрузились
                            echo "DB_HOST=\${DB_HOST}"
                            echo "DB_PASSWORD=\${DB_PASSWORD}"

                            # Создаем Secret (чувствительные данные)
                            kubectl create secret generic bot-secrets \\
                                --from-literal=DB_PASSWORD="\${DB_PASSWORD}" \\
                                --from-literal=BOT_TOKEN="\${BOT_TOKEN}" \\
                                --from-literal=ADMIN_PASSWORD="\${ADMIN_PASSWORD}" \\
                                --from-literal=API_KEY="\${API_KEY}" \\
                                -n \${NAMESPACE} \\
                                --dry-run=client -o yaml | kubectl apply -f -

                            # Создаем ConfigMap (остальные данные)
                            # DB_URL формируем явно, так как K8s не делает интерполяцию внутри значений
                            DB_URL_VAL="jdbc:postgresql://\${DB_HOST}:\${DB_PORT}/\${DB_NAME}"

                            kubectl create configmap bot-config \\
                                --from-literal=DB_HOST="\${DB_HOST}" \\
                                --from-literal=DB_PORT="\${DB_PORT}" \\
                                --from-literal=DB_NAME="\${DB_NAME}" \\
                                --from-literal=DB_USERNAME="\${DB_USERNAME}" \\
                                --from-literal=DB_URL="\${DB_URL_VAL}" \\
                                --from-literal=HTTP_PORT="\${HTTP_PORT}" \\
                                --from-literal=HTTP_HOST="\${HTTP_HOST}" \\
                                --from-literal=BOT_USERNAME="\${BOT_USERNAME}" \\
                                -n \${NAMESPACE} \\
                                --dry-run=client -o yaml | kubectl apply -f -

                            echo "✅ Secrets and ConfigMaps created."
                        """

                        // Применяем манифесты Postgres и Приложения
                        echo '🚀 Applying Manifests...'
                        sh """
                            kubectl apply -f \${K8S_DIR}/postgres.yaml -n \${NAMESPACE}
                            kubectl apply -f \${K8S_DIR}/app.yaml -n \${NAMESPACE}
                        """

                        // Ждем запуска
                        echo '⏳ Waiting for PostgreSQL...'
                        sh "kubectl rollout status deployment/postgres -n \${NAMESPACE} --timeout=120s"

                        echo '⏳ Waiting for Movie Bot...'
                        sh "kubectl rollout status deployment/movie-bot -n \${NAMESPACE} --timeout=120s"
                    }
                }
            }
        }

        stage('Check Status') {
            steps {
                script {
                    sh """
                        echo "📜 Pods status:"
                        kubectl get pods -n ${NAMESPACE}
                        echo "🌐 Services:"
                        kubectl get svc -n ${NAMESPACE}
                    """
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}