pipeline {
    agent any

    environment {
        DB_PASSWORD = credentials('DB_PASSWORD')
        DB_NAME = 'users_db'
        DB_USER = 'postgres'  // Должно совпадать с POSTGRES_USER в docker run
        DB_CONTAINER_NAME = "pg_${JOB_NAME}_${BUILD_ID}".replaceAll('[^a-zA-Z0-9_]', '_')
        // Используем имя контейнера как хост для подключения из Maven/jOOQ
        DB_HOST = "${DB_CONTAINER_NAME}"
    }

    stages {
        stage('Start Database') {
            steps {
                script {
                    withCredentials([string(credentialsId: 'DB_PASSWORD', variable: 'DB_PASSWORD_SAFE')]) {
                        withEnv([
                                "DB_CONTAINER_NAME=${env.DB_CONTAINER_NAME}",
                                "DB_PASSWORD=${env.DB_PASSWORD}",
                                "DB_NAME=${env.DB_NAME}",
                                "DB_USER=${env.DB_USER}"
                        ]) {
                            sh '''
                                export DOCKER_HOST=unix:///var/run/docker.sock
                                export DOCKER_TLS_VERIFY=""
                                export DOCKER_CERT_PATH=""
                                
                                # Запуск PostgreSQL с фиксированным именем для доступа по DNS
                                docker run -d \
                                    --name "${DB_CONTAINER_NAME}" \
                                    -e POSTGRES_PASSWORD="${DB_PASSWORD_SAFE}" \
                                    -e POSTGRES_DB="${DB_NAME}" \
                                    -e POSTGRES_USER="${DB_USER}" \
                                    -p 5432:5432 \
                                    postgres:15

                                # Ожидание готовности
                                echo "Waiting for PostgreSQL..."
                                until docker exec "${DB_CONTAINER_NAME}" pg_isready -U "${DB_USER}"; do
                                    sleep 2
                                done
                            '''
                        }
                    }
                }
            }
        }

        stage('Database Setup') {
            steps {
                script {
                    withCredentials([string(credentialsId: 'DB_PASSWORD', variable: 'DB_PASSWORD_SAFE')]) {
                        withEnv([
                                "DB_CONTAINER_NAME=${env.DB_CONTAINER_NAME}",
                                "DB_PASSWORD=${env.DB_PASSWORD}",
                                "DB_NAME=${env.DB_NAME}",
                                "DB_USER=${env.DB_USER}",
                                "WORKSPACE=${env.WORKSPACE}"
                        ]) {
                            sh '''
                                cd "${WORKSPACE}/MovieRecommendationBot"
                                SQL_FILE="src/main/resources/users_db.sql"
                                
                                if [ -f "${SQL_FILE}" ]; then
                                    cat "${SQL_FILE}" | docker exec -i \
                                        -e PGPASSWORD="${DB_PASSWORD_SAFE}" \
                                        "${DB_CONTAINER_NAME}" \
                                        psql -U "${DB_USER}" -d "${DB_NAME}"
                                else
                                    echo "File not found: ${SQL_FILE}"
                                    exit 1
                                fi
                            '''
                        }
                    }
                }
            }
        }

        stage('Build & jOOQ') {
            steps {
                script {
                    withCredentials([string(credentialsId: 'DB_PASSWORD', variable: 'DB_PASSWORD_SAFE')]) {
                        withEnv([
                                "DB_HOST=${env.DB_HOST}",
                                "DB_PASSWORD=${env.DB_PASSWORD}",
                                "DB_USER=${env.DB_USER}",
                                "DB_NAME=${env.DB_NAME}",
                                "WORKSPACE=${env.WORKSPACE}"
                        ]) {
                            sh '''
                                cd "${WORKSPACE}/MovieRecommendationBot"
                                echo "🔹 Building with DB_HOST=${DB_HOST}"
                                
                                # Передаём параметры подключения к БД в Maven
                                mvn clean package \
                                  -DskipTests \
                                  -Ddb.password="${DB_PASSWORD_SAFE}" \
                                  -Ddb.host="${DB_HOST}" \
                                  -Ddb.user="${DB_USER}" \
                                  -Ddb.name="${DB_NAME}" \
                                  -Dstyle.color=always
                            '''
                        }
                    }
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'MovieRecommendationBot/target/MovieRecommendationBot-*.jar', fingerprint: true, allowEmptyArchive: true
                }
            }
        }
    }

    post {
        always {
            script {
                withEnv(["DB_CONTAINER_NAME=${env.DB_CONTAINER_NAME}"]) {
                    sh '''
                        echo "Cleaning up container: ${DB_CONTAINER_NAME}"
                        export DOCKER_HOST=unix:///var/run/docker.sock
                        export DOCKER_TLS_VERIFY=""
                        export DOCKER_CERT_PATH=""
                        docker rm -f "${DB_CONTAINER_NAME}" 2>/dev/null || echo "Container ${DB_CONTAINER_NAME} not found"
                    '''
                }
                dir(env.WORKSPACE) {
                    deleteDir()
                }
            }
        }
        failure {
            echo "\033[31m Pipeline failed! Check console output.\033[0m"
        }
        success {
            echo "\033[32m Success! Build completed.\033[0m"
        }
    }
}