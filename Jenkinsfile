pipeline {
    agent any

    environment {
        PROJECT_DIR = 'MovieRecommendationBot'
        K8S_DIR = 'MovieRecommendationBot/k8s'
        IMAGE_NAME = 'movie-bot:latest'
        NAMESPACE = 'default'
        CLUSTER_NAME = 'movie-bot-cluster'
    }

    stages {
        // ==========================================
        // ЭТАП 1: СБОРКА И DOCKER ОБРАЗ
        // ==========================================
        stage('Build & Dockerize') {
            steps {
                script {
                    echo '🔨 Building Application...'

                    // 1. Временная БД для сборки (jOOQ)
                    sh """
                        docker run -d --name build-db -e POSTGRES_PASSWORD=password -e POSTGRES_DB=users_db -p 54321:5432 postgres:15
                        sleep 15
                        cat ${PROJECT_DIR}/src/main/resources/users_db.sql | docker exec -i build-db psql -U postgres -d users_db || true
                    """

                    // 2. Сборка JAR
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
                    echo '🐳 Building Docker Image...'
                    sh "docker build -t ${IMAGE_NAME} ${PROJECT_DIR}/"

                    // 4. ВАЖНО: Загружаем образ в кластер Kind!
                    // Без этого шага K8s не увидит локальный образ из Jenkins
                    echo '📦 Loading image into Kind cluster...'
                    sh """
                        kind load docker-image ${IMAGE_NAME} --name ${CLUSTER_NAME}
                    """
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

                        // Парсим .env файл безопасно для Groovy
                        sh """
                            # 1. Создаем чистый файл без комментариев и пустых строк
                            # Используем простой grep, чтобы избежать проблем с экранированием в Groovy
                            grep '=' \${ENV_FILE_PATH} | grep -v '^#' | sed '/^\$/d' > /tmp/clean.env

                            # 2. Включаем автоматический экспорт переменных
                            set -a

                            # 3. Загружаем переменные (точка . работает в sh как source)
                            . /tmp/clean.env

                            # 4. Выключаем авто-экспорт
                            set +a

                            echo "✅ Variables loaded. DB_HOST=\${DB_HOST}"

                            # Создаем Secret
                            kubectl create secret generic bot-secrets \\
                                --from-literal=DB_PASSWORD="\${DB_PASSWORD}" \\
                                --from-literal=BOT_TOKEN="\${BOT_TOKEN}" \\
                                --from-literal=ADMIN_PASSWORD="\${ADMIN_PASSWORD}" \\
                                --from-literal=API_KEY="\${API_KEY}" \\
                                -n \${NAMESPACE} \\
                                --dry-run=client -o yaml | kubectl apply -f -

                            # Создаем ConfigMap
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

                            rm -f /tmp/clean.env
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