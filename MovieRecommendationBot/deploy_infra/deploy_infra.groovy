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
        TF_VAR_zone         = 'ru-central1-a'

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
                        for (int i = 0; i < 40; i++) {  // 40 * 15 сек = 10 минут
                            def result = sh(
                                    script: "ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 -o BatchMode=yes ubuntu@${env.SERVER_IP} 'echo SSH ready' 2>&1",
                                    returnStatus: true
                            )
                            if (result == 0) {
                                echo "✅ SSH ready"
                                return
                            }
                            echo "⏳ Attempt ${i+1}/40..."
                            sleep(time: 15, unit: 'SECONDS')
                        }
                        error("SSH timeout after 10 minutes")
                    }
                }
            }
        }

        stage('Prepare SSH Key for Ansible') {
            steps {
                script {
                    // Создаём временный файл для приватного ключа
                    env.SSH_KEY_PATH = "${env.WORKSPACE}/.ssh/deploy_key"
                    sh '''
                        mkdir -p "${WORKSPACE}/.ssh"
                        echo "${SSH_KEY}" > "${SSH_KEY_PATH}"
                        chmod 600 "${SSH_KEY_PATH}"
                    '''
                    echo "✅ SSH key prepared at ${env.SSH_KEY_PATH}"
                }
            }
        }

        stage('Ansible Deploy') {
            steps {
                script {
                    // Генерируем динамический inventory с правильным путём к ключу
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
                                  ansible_ssh_private_key_file: ${env.SSH_KEY_PATH}
                                  ansible_python_interpreter: /usr/bin/python3
                        EOF
                    """

                    // Запуск playbook с динамическим inventory
                    dir("${ANSIBLE_DIR}") {
                        sh "ansible-playbook -i inventory_dynamic.yml playbook.yml"
                    }

                    // Очистка временного inventory
                    sh "rm -f ${ANSIBLE_DIR}/inventory_dynamic.yml"
                }
                echo "✅ Application deployed"
            }
        }

        stage('Health Check') {
            steps {
                script {
                    echo "🔍 Checking application health..."
                    timeout(time: 5, unit: 'MINUTES') {
                        def response = sh(
                                script: "curl -s -o /dev/null -w '%{http_code}' http://${env.SERVER_IP}:8110/health || echo '000'",
                                returnStdout: true
                        ).trim()

                        if (response ==~ /200|404/) {
                            echo "✅ Application responding (HTTP ${response})"
                        } else {
                            echo "⚠️ Application check returned HTTP ${response}"
                        }
                    }
                }
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
                // Очистка временного SSH-ключа
                if (env.SSH_KEY_PATH) {
                    sh "rm -f ${env.SSH_KEY_PATH} 2>/dev/null || true"
                }
                // Очистка workspace
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