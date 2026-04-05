pipeline {
    agent {
        docker {
            image 'postgres:15'
            args '-v /var/run/docker.sock:/var/run/docker.sock'
        }
    }

    environment {
        DB_PASSWORD = credentials('DB_PASSWORD')
        DB_NAME = 'users_db'
        DB_USER = 'postgres'
        DB_CONTAINER_NAME = "pg_${BUILD_ID}"
    }

    stages {
        stage('Database Setup') {
            steps {
                sh '''
                    # 1. Запуск контейнера PostgreSQL в фоновом режиме
                    docker run -d \
                        --name ${DB_CONTAINER_NAME} \
                        -e POSTGRES_PASSWORD=${DB_PASSWORD} \
                        -e POSTGRES_DB=${DB_NAME} \
                        -e POSTGRES_USER=${DB_USER} \
                        -p 5432:5432 \
                        postgres:15

                    # 2. Ожидание готовности БД (проверка TCP-порта)
                    echo "Waiting for PostgreSQL to be ready..."
                    until docker exec ${DB_CONTAINER_NAME} pg_isready -U ${DB_USER}; do
                        sleep 2
                    done

                    # 3. Применение SQL-скрипта
                    cd "${WORKSPACE}/MovieRecommendationBot"
                    SQL_FILE="src/main/resources/users_db.sql"
                    
                    if [ -f "$SQL_FILE" ]; then
                        docker exec -e PGPASSWORD=${DB_PASSWORD} ${DB_CONTAINER_NAME} \
                            psql -U ${DB_USER} -d ${DB_NAME} -f /workspace/MovieRecommendationBot/$SQL_FILE
                    else
                        echo "File not found: $SQL_FILE"
                        exit 1
                    fi
                '''
            }
        }

        stage('Build & jOOQ') {
            steps {
                sh '''
                    cd "${WORKSPACE}/MovieRecommendationBot"
                    echo "🔹 DB_PASSWORD is set: [${DB_PASSWORD:+***SET***}]"
                    
                    # Передаем параметры подключения к БД в Maven
                    # Хост 'localhost', так как мы в той же сети контейнера
                    mvn clean package \
                      -DskipTests \
                      -Ddb.password=${DB_PASSWORD} \
                      -Ddb.host=localhost \
                      -Ddb.user=${DB_USER} \
                      -Ddb.name=${DB_NAME} \
                      -Dstyle.color=always
                '''
            }
            post {
                success {
                    archiveArtifacts artifacts: 'MovieRecommendationBot/target/MovieRecommendationBot-*.jar', fingerprint: true
                }
            }
        }
    }

    post {
        always {
            sh "docker rm -f ${DB_CONTAINER_NAME} 2>/dev/null || true"
            deleteDir()
        }
        failure {
            echo "\033[31m Pipeline failed! Check console output.\033[0m"
        }
        success {
            echo "\033[32m Success! Build completed.\033[0m"
        }
    }
}