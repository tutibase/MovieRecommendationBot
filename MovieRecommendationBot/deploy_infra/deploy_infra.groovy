pipeline {
    agent any

    environment {
        TF_DIR = 'MovieRecommendationBot/terraform'
        ANSIBLE_DIR = 'MovieRecommendationBot/ansible'
        APP_DIR = '/opt/movie-bot'
        TF_PLUGIN_CACHE_DIR = '/var/jenkins_home/.terraform.d/plugin-cache'

        // Terraform variables (значения по умолчанию, будут переопределены через withCredentials)
        TF_VAR_cloud_id     = ''
        TF_VAR_folder_id    = ''
        TF_VAR_yc_token     = ''
        TF_VAR_ssh_public_key = ''
        TF_VAR_zone         = 'ru-central1-d'

        // Ansible settings
        ANSIBLE_HOST_KEY_CHECKING = 'False'
    }

    options {
        disableConcurrentBuilds()
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                echo "✅ Repository checked out"
            }
        }

        stage('Terraform Init') {
            steps {
                dir("${TF_DIR}") {
                    sh '''
                # Создаём директорию кэша, если не существует
                mkdir -p ${TF_PLUGIN_CACHE_DIR}
                
                # Инициализация с использованием локального кэша
                # -get-plugins=false предотвращает попытку скачивания, если провайдер уже есть
                terraform init -input=false -no-color -get-plugins=false || \
                terraform init -input=false -no-color
            '''
                }
                echo "✅ Terraform initialized"
            }
        }

        stage('Terraform Plan') {
            steps {
                // Передаём секреты через withCredentials для безопасности
                withCredentials([
                        string(credentialsId: 'yc-token', variable: 'TF_VAR_yc_token'),
                        string(credentialsId: 'yc-cloud-id', variable: 'TF_VAR_cloud_id'),
                        string(credentialsId: 'yc-folder-id', variable: 'TF_VAR_folder_id'),
                        string(credentialsId: 'ssh-public-key', variable: 'TF_VAR_ssh_public_key')
                ]) {
                    dir("${TF_DIR}") {
                        // Одинарные кавычки ''' предотвращают интерполяцию Groovy
                        sh '''
                            terraform plan -no-color \
                                -var="cloud_id=${TF_VAR_cloud_id}" \
                                -var="folder_id=${TF_VAR_folder_id}" \
                                -var="yc_token=${TF_VAR_yc_token}" \
                                -var="ssh_public_key=${TF_VAR_ssh_public_key}"
                        '''
                    }
                }
                echo "✅ Terraform plan generated"
            }
        }

        stage('Terraform Apply') {
            steps {
                withCredentials([
                        string(credentialsId: 'yc-token', variable: 'TF_VAR_yc_token'),
                        string(credentialsId: 'yc-cloud-id', variable: 'TF_VAR_cloud_id'),
                        string(credentialsId: 'yc-folder-id', variable: 'TF_VAR_folder_id'),
                        string(credentialsId: 'ssh-public-key', variable: 'TF_VAR_ssh_public_key')
                ]) {
                    dir("${TF_DIR}") {
                        sh '''
                            terraform apply -auto-approve -no-color \
                                -var="cloud_id=${TF_VAR_cloud_id}" \
                                -var="folder_id=${TF_VAR_folder_id}" \
                                -var="yc_token=${TF_VAR_yc_token}" \
                                -var="ssh_public_key=${TF_VAR_ssh_public_key}" \
                                -var="zone=${TF_VAR_zone}"
                        '''
                    }
                }
                echo "✅ Infrastructure deployed"
            }
        }

        stage('Extract Server IP') {
            steps {
                script {
                    // IP извлекается без передачи секретов
                    env.SERVER_IP = sh(
                            script: "cd ${TF_DIR} && terraform output -raw instance_public_ip",
                            returnStdout: true
                    ).trim()
                    echo "Server IP: ${env.SERVER_IP}"
                }
            }
        }

        stage('Wait for SSH') {
            steps {
                script {
                    echo "Waiting for SSH on ${env.SERVER_IP}..."
                    timeout(time: 10, unit: 'MINUTES') {
                        // Используем withCredentials для безопасного доступа к ключу
                        withCredentials([sshUserPrivateKey(
                                credentialsId: 'ssh-private-key',
                                keyFileVariable: 'SSH_KEY_FILE',
                                usernameVariable: 'SSH_USER',
                                passphraseVariable: ''
                        )]) {
                            for (int i = 0; i < 40; i++) {  // 40 * 15 сек = 10 минут
                                def result = sh(
                                        script: """
                                ssh -i \${SSH_KEY_FILE} \\
                                    -o StrictHostKeyChecking=no \\
                                    -o ConnectTimeout=10 \\
                                    -o BatchMode=yes \\
                                    \${SSH_USER:-ubuntu}@${env.SERVER_IP} 'echo SSH ready' 2>&1
                            """,
                                        returnStatus: true
                                )
                                if (result == 0) {
                                    echo "✅ SSH ready"
                                    return
                                }
                                echo "⏳ Attempt ${i+1}/40..."
                                sleep(time: 15, unit: 'SECONDS')
                            }
                        }
                        error("SSH timeout after 10 minutes")
                    }
                }
            }
        }

        stage('Ansible Deploy') {
            steps {
                sh """
                cat > ${ANSIBLE_DIR}/inventory_dynamic.yml << EOF
                ---
                all:
                  children:
                    movie_bot_vms:
                      hosts:
                        poly-bot-vm:
                          ansible_host: ${env.SERVER_IP}
                          ansible_user: ubuntu
                          ansible_python_interpreter: /usr/bin/python3
                EOF
                """

                withCredentials([sshUserPrivateKey(
                        credentialsId: 'ssh-private-key',
                        keyFileVariable: 'SSH_KEY_FILE',
                        usernameVariable: 'SSH_USER',
                        passphraseVariable: ''
                )]) {
                    dir("${ANSIBLE_DIR}") {
                        sh """
                    ansible-playbook -i inventory_dynamic.yml playbook.yml \\
                        --private-key \${SSH_KEY_FILE} \\
                        -u \${SSH_USER:-ubuntu} \\
                        -vv
                        """
                    }
                }

                sh "rm -f ${ANSIBLE_DIR}/inventory_dynamic.yml"

                echo "✅ Application deployed"
            }
        }

        stage('Archive Artifacts') {
            steps {
                archiveArtifacts artifacts: 'terraform/*.tf, ansible/**/*.yml, ansible/hosts.ini', allowEmptyArchive: true
                echo "✅ Artifacts archived"
            }
        }
    }

    post {
        always {
            script {
                cleanWs()
            }
        }

        success {
            echo "Pipeline completed successfully!"
            echo "Application URL: http://${env.SERVER_IP}:8110"
            echo "SSH: ssh -i <private-key> ubuntu@${env.SERVER_IP}"
        }

        failure {
            echo "Pipeline failed. Check console output for details."
            echo "To cleanup: cd terraform && terraform destroy -auto-approve"
        }
    }
}