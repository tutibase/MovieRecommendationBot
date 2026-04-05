pipeline {
    agent any

    environment {
        DB_PASSWORD = credentials('DB_PASSWORD')
    }

    // Запуск дополнительного контейнера с PostgreSQL
    services {
        postgres {
            image 'postgres:15'
            environment {
                POSTGRES_USER: 'postgres'
                POSTGRES_PASSWORD: "${DB_PASSWORD}"
                POSTGRES_DB: 'users_db'
            }
            ports {
                innerPort 5432
                outerPort 5432
            }
        }
    }

    environment {
        DB_PASSWORD = credentials('DB_PASSWORD')
        // Хост для подключения внутри сети Docker
        DB_HOST = 'postgres'
        DB_USER = 'postgres'
    }

    stages {
        stage('Database Setup') {
            steps {
                sh '''
                    cd "${WORKSPACE}/MovieRecommendationBot"
                    SQL_FILE="src/main/resources/users_db.sql"
                    
                    if [ -f "$SQL_FILE" ]; then
                        # Ждем готовности БД
                        until pg_isready -h ${DB_HOST} -U ${DB_USER}; do
                            echo "Waiting for database..."
                            sleep 2
                        done
                        
                        # Применяем скрипт
                        PGPASSWORD=${DB_PASSWORD} psql -h ${DB_HOST} -U ${DB_USER} -d users_db -f "$SQL_FILE"
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
                    mvn clean package \
                      -DskipTests \
                      -Ddb.password=$DB_PASSWORD \
                      -Dstyle.color=always
                '''
            }
            post {
                success {
                    archiveArtifacts artifacts: 'MovieRecommendationBot/target/MovieRecommendationBot-*.jar', fingerprint: true
                }
                always {
                    sh "docker rm -f \$(docker ps -a -q --filter name=tc-) 2>/dev/null || true"
                }
            }
        }
    }

    post {
        always {
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