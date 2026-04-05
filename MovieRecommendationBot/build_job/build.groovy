pipeline {
    agent any

    environment {
        DB_PASSWORD = credentials('DB_PASSWORD')
        DB_NAME = 'users_db'
        DB_USER = 'postgres'
        DB_CONTAINER_PREFIX = "pg_${JOB_NAME}_${BUILD_ID}".replaceAll('[^a-zA-Z0-9_]', '_')
    }

    stages {
        stage('Database Setup') {
            steps {
                script {
                    sh(script: '''
                        export DOCKER_HOST=unix:///var/run/docker.sock
                        export DOCKER_TLS_VERIFY=""
                        export DOCKER_CERT_PATH=""
                        
                        docker run -d \
                            --name "${DB_CONTAINER_PREFIX}" \
                            -e POSTGRES_PASSWORD="${DB_PASSWORD}" \
                            -e POSTGRES_DB="${DB_NAME}" \
                            -e POSTGRES_USER="${DB_USER}" \
                            -p 5432:5432 \
                            postgres:15

                        echo "Waiting for PostgreSQL..."
                        until docker exec "${DB_CONTAINER_PREFIX}" pg_isready -U "${DB_USER}"; do
                            sleep 2
                        done

                        cd "${WORKSPACE}/MovieRecommendationBot"
                        SQL_FILE="src/main/resources/users_db.sql"
                        
                        if [ -f "${SQL_FILE}" ]; then
                            docker exec -e PGPASSWORD="${DB_PASSWORD}" "${DB_CONTAINER_PREFIX}" \
                                psql -U "${DB_USER}" -d "${DB_NAME}" -f "/workspace/MovieRecommendationBot/${SQL_FILE}"
                        else
                            echo "File not found: ${SQL_FILE}"
                            exit 1
                        fi
                    ''', environment: [
                            DB_CONTAINER_PREFIX: env.DB_CONTAINER_PREFIX,
                            DB_PASSWORD: env.DB_PASSWORD,
                            DB_NAME: env.DB_NAME,
                            DB_USER: env.DB_USER,
                            WORKSPACE: env.WORKSPACE
                    ])
                }
            }
        }

        stage('Build & jOOQ') {
            steps {
                sh(script: '''
                    cd "${WORKSPACE}/MovieRecommendationBot"
                    echo "🔹 DB_PASSWORD is set"
                    
                    mvn clean package \
                      -DskipTests \
                      -Ddb.password="${DB_PASSWORD}" \
                      -Ddb.host=localhost \
                      -Ddb.user="${DB_USER}" \
                      -Ddb.name="${DB_NAME}" \
                      -Dstyle.color=always
                ''', environment: [
                        DB_PASSWORD: env.DB_PASSWORD,
                        DB_USER: env.DB_USER,
                        DB_NAME: env.DB_NAME,
                        WORKSPACE: env.WORKSPACE
                ])
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
                def containerName = "pg_${env.JOB_NAME}_${env.BUILD_ID}".replaceAll('[^a-zA-Z0-9_]', '_')

                sh(script: '''
                    echo "Cleaning up container: ${containerName}"
                    export DOCKER_HOST=unix:///var/run/docker.sock
                    export DOCKER_TLS_VERIFY=""
                    export DOCKER_CERT_PATH=""
                    docker rm -f "${containerName}" 2>/dev/null || echo "Container ${containerName} not found"
                ''', environment: [
                        containerName: containerName
                ])

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