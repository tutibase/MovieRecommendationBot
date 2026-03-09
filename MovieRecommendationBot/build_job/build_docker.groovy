pipeline {
    agent {
        label 'poly-agent'
    }

    options {
        ansiColor('xterm')
    }

    environment {
        DOCKER_IMAGE = "polyalugovenko/movie-recommendation-bot"
        IMAGE_TAG = "build-${env.BUILD_NUMBER}"
        DOCKER_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Get Artifact') {
            steps {
                echo "Copying artifact from Lab2-Build..."
                copyArtifacts projectName: 'Lab2-Build',
                        selector: lastSuccessful(),
                        target: 'target/',
                        filter: '**/*.jar',
                        flatten: true
            }
        }

        stage('Build Image') {
            steps {
                script {
                    echo "Building Docker image: ${DOCKER_IMAGE}:${IMAGE_TAG}"
                    sh "docker build -t ${DOCKER_IMAGE}:${IMAGE_TAG} ."
                    env.DOCKER_IMAGE_BUILT = "${DOCKER_IMAGE}:${IMAGE_TAG}"
                }
            }
            post {
                always {
                    sh "docker rmi ${DOCKER_IMAGE}:${IMAGE_TAG} ${DOCKER_IMAGE}:latest 2>/dev/null || true"
                }
            }
        }

        stage('Tag & Push') {
            steps {
                script {
                    echo "Pushing to Docker Hub..."

                    withCredentials([usernamePassword(
                            credentialsId: 'dockerhub-credentials',
                            usernameVariable: 'DOCKER_USER',
                            passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh "echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin"
                        sh "docker push ${DOCKER_IMAGE}:${IMAGE_TAG}"

                        echo "Tagging as latest..."
                        sh "docker tag ${DOCKER_IMAGE}:${IMAGE_TAG} ${DOCKER_IMAGE}:latest"
                        sh "docker push ${DOCKER_IMAGE}:latest"

                        sh "docker logout"
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        failure {
            echo "\033[31m Pipeline failed! Check console output.\033[0m"
        }
        success {
            echo "\033[32m Success! Image pushed:\033[0m"
            echo "\033[36m https://hub.docker.com/r/${DOCKER_IMAGE}/tags\033[0m"
        }
    }
}