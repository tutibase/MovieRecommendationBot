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

                        echo '📄 Parsing .env and Deploying...'

                        sh """
                            # 1. Чистим файл от Windows-переносов строк (\\r) и комментариев
                            cat \${ENV_FILE_PATH} | tr -d '\\r' | grep '=' | grep -v '^#' | sed '/^\$/d' > /tmp/clean.env

                            # 2. Загружаем переменные
                            set -a
                            . /tmp/clean.env
                            set +a

                            echo "✅ Variables loaded."

                            # 3. Собираем URL вручную из чистых компонентов
                            CLEAN_DB_URL="jdbc:postgresql://\${DB_HOST}:\${DB_PORT}/\${DB_NAME}"

                            # 4. Создаем/Обновляем Secret
                            kubectl create secret generic bot-secrets \\
                                --from-literal=DB_PASSWORD="\${DB_PASSWORD}" \\
                                --from-literal=BOT_TOKEN="\${BOT_TOKEN}" \\
                                --from-literal=ADMIN_PASSWORD="\${ADMIN_PASSWORD}" \\
                                --from-literal=API_KEY="\${API_KEY}" \\
                                -n \${NAMESPACE} \\
                                --dry-run=client -o yaml | kubectl apply -f -

                            # 5. Создаем/Обновляем ConfigMap
                            kubectl create configmap bot-config \\
                                --from-literal=DB_HOST="\${DB_HOST}" \\
                                --from-literal=DB_PORT="\${DB_PORT}" \\
                                --from-literal=DB_NAME="\${DB_NAME}" \\
                                --from-literal=DB_USERNAME="\${DB_USERNAME}" \\
                                --from-literal=DB_URL="\${CLEAN_DB_URL}" \\
                                --from-literal=HTTP_PORT="\${HTTP_PORT}" \\
                                --from-literal=HTTP_HOST="0.0.0.0" \\
                                --from-literal=BOT_USERNAME="\${BOT_USERNAME}" \\
                                -n \${NAMESPACE} \\
                                --dry-run=client -o yaml | kubectl apply -f -

                            echo "✅ Configs updated."

                            # 6. ВАЖНО: Сначала применяем манифесты (create/update)
                            # Укажите правильный путь к вашим файлам.
                            # Если они в папке k8s внутри проекта:
                            echo "🚀 Applying Kubernetes Manifests..."

                            # Применяем БД
                            kubectl apply -f ${K8S_DIR}/postgre.yaml -n ${NAMESPACE}

                            # Применяем Приложение
                            kubectl apply -f ${K8S_DIR}/app.yaml -n ${NAMESPACE}

                            # 7. Форсируем перезапуск подов приложения, чтобы подтянуть новый образ :latest
                            # Теперь эта команда сработает, так как мы только что сделали apply
                            echo "🔄 Restarting deployment to pick up new image..."
                            kubectl rollout restart deployment/movie-bot -n \${NAMESPACE}

                            # 8. Ждем готовности
                            echo "⏳ Waiting for rollout to complete..."
                            kubectl rollout status deployment/movie-bot -n \${NAMESPACE} --timeout=120s

                            rm -f /tmp/clean.env
                        """
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