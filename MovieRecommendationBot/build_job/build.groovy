pipeline {
    agent {
        label 'poly-agent'
    }

    options {
        ansiColor('xterm')
    }

    environment {
        DB_PASSWORD = credentials('POSTGRES_DB_PASSWORD')
    }

    stages {

        stage('Database Setup') {
            steps {
                // Используем права jenkins-poly (без sudo)
                sh '''
                    createdb users_db 2>/dev/null || echo "DB exists"
                    psql -d users_db -f "$(pwd)/src/main/resources/users_db.sql"
                '''
            }
        }

        stage('Build & jOOQ') {
            steps {
                // Testcontainers сам поднимет докер-контейнер для генерации кода
                sh "mvn clean package -DskipTests -Ddb.password=${DB_PASSWORD} -Dstyle.color=always"
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/MovieRecommendationBot-*.jar', fingerprint: true
                }
                always {
                    sh "docker rm -f \$(docker ps -a -q --filter name=tc-) 2>/dev/null || true"
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