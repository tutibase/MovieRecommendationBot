pipeline {
    agent any

    environment {
        YC_TOKEN      = credentials('yc-iam-token')
        YC_CLOUD_ID   = credentials('yc-cloud-id')
        YC_FOLDER_ID  = credentials('yc-folder-id')
        SSH_KEY_FILE  = '/var/jenkins_home/.ssh/id_rsa'
        DB_PASSWORD   = credentials('db-password')
        ENV_FILE_PATH = credentials('bot-env-file')

        TF_DIR        = 'infra/terraform'
        ANSIBLE_DIR   = 'infra/ansible'
        APP_DIR       = '/opt/moviebot'
        PROJECT_DIR   = 'MovieRecommendationBot'

        VM_IP         = ''
        JAR_FILE      = ''
    }

    stages {
        stage('Build Application') {
            steps {
                script {
                    echo '🚀 Starting Build Stage...'

                    // 0. Очистка
                    sh 'docker rm -f build-db || true'
                    sh 'sleep 2'

                    // 1. Подъем БД и импорт схемы (ОДИН блок sh)
                    sh """
                        docker run -d --name build-db \\
                            -e POSTGRES_PASSWORD=${DB_PASSWORD} \\
                            -e POSTGRES_DB=users_db \\
                            -p 54321:5432 \\
                            postgres:15

                        echo "⏳ Waiting for DB to be ready..."
                        sleep 20

                        echo "Checking databases..."
                        docker exec build-db psql -U postgres -c "\\l" | grep users_db

                        echo "Importing schema..."
                        cat ${PROJECT_DIR}/src/main/resources/users_db.sql | \\
                            docker exec -i build-db psql -U postgres -d users_db

                        echo "✅ Schema imported."
                    """

                    // 2. ДИАГНОСТИКА (ОТДЕЛЬНЫЙ шаг sh)
                    echo '🔍 Testing TCP connection...'
                    sh '''
                        # Проверка резолвинга
                        getent hosts host.docker.internal || echo "❌ Cannot resolve host.docker.internal"

                        # Проверка порта через bash (nc может не быть)
                        if timeout 5 bash -c 'echo > /dev/tcp/host.docker.internal/5432' 2>/dev/null; then
                            echo "✅ TCP Port 5432 is OPEN"
                        else
                            echo "❌ TCP Port 5432 is CLOSED or Unreachable"
                        fi
                    '''

                    // 3. Maven Сборка
                    dir("${PROJECT_DIR}") {
                        sh """
                            echo "🔨 Starting Maven Build..."
                            # Добавлен флаг -e для полного вывода ошибки
                            mvn clean package -DskipTests -e \\
                                -Ddb.password=${DB_PASSWORD} \\
                                -Ddb.url=jdbc:postgresql://host.docker.internal:54321/users_db \\
                                -Ddb.user=postgres
                        """
                    }

                    // 4. Поиск JAR
                    script {
                        def jars = findFiles(glob: "${PROJECT_DIR}/target/MovieRecommendationBot-*.jar")
                        JAR_FILE = jars.find { !it.name.contains('original') }?.path
                        if (!JAR_FILE) {
                            error "❌ JAR file not found!"
                        }
                        echo "✅ Found JAR: ${JAR_FILE}"
                    }

                    // 5. Очистка БД
                    sh 'docker rm -f build-db'
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: "${PROJECT_DIR}/target/*.jar", fingerprint: true
                }
                always {
                    sh 'docker rm -f build-db || true'
                }
            }
        }

        stage('Create Infrastructure') {
            steps {
                echo '☁️ Creating Infrastructure...'
                dir("${TF_DIR}") {
                    withCredentials([
                        string(credentialsId: 'yc-iam-token', variable: 'TF_VAR_yc_token'),
                        string(credentialsId: 'yc-cloud-id', variable: 'TF_VAR_cloud_id'),
                        string(credentialsId: 'yc-folder-id', variable: 'TF_VAR_folder_id'),
                        string(credentialsId: 'ssh-public-key', variable: 'TF_VAR_ssh_public_key')
                    ]) {
                        sh '''
                            terraform init -input=false
                            terraform apply -auto-approve -input=false \\
                                -var="yc_token=${TF_VAR_yc_token}" \\
                                -var="cloud_id=${TF_VAR_cloud_id}" \\
                                -var="folder_id=${TF_VAR_folder_id}" \\
                                -var="ssh_public_key=${TF_VAR_ssh_public_key}"
                        '''
                    }
                }
            }
        }

        stage('Provision Server') {
            steps {
                script {
                    echo '⚙️ Provisioning Server...'
                    VM_IP = sh(script: "cd ${TF_DIR} && terraform output -raw vm_public_ip", returnStdout: true).trim()
                    echo "📍 VM IP: ${VM_IP}"

                    sh """
                        echo "[all]" > ${ANSIBLE_DIR}/inventory.ini
                        echo "${VM_IP} ansible_user=ubuntu ansible_ssh_private_key_file=${SSH_KEY_FILE}" >> ${ANSIBLE_DIR}/inventory.ini
                    """

                    dir("${ANSIBLE_DIR}") {
                        sh """
                            ansible-playbook -i inventory.ini playbook.yml \\
                                --private-key ${SSH_KEY_FILE} \\
                                --extra-vars "db_password=${DB_PASSWORD} ansible_ssh_common_args='-o StrictHostKeyChecking=no'"
                        """
                    }
                }
            }
        }

        stage('Deploy Artifact') {
            steps {
                script {
                    echo '📦 Deploying...'
                    if (!JAR_FILE) { error "❌ JAR_FILE is empty!" }

                    sh """
                        scp -o StrictHostKeyChecking=no -i ${SSH_KEY_FILE} \\
                            ${JAR_FILE} ubuntu@${VM_IP}:${APP_DIR}/MovieRecommendationBot.jar

                        scp -o StrictHostKeyChecking=no -i ${SSH_KEY_FILE} \\
                            ${ENV_FILE_PATH} ubuntu@${VM_IP}:${APP_DIR}/.env

                        ssh -o StrictHostKeyChecking=no -i ${SSH_KEY_FILE} \\
                            ubuntu@${VM_IP} "sudo systemctl daemon-reload && sudo systemctl restart moviebot"
                    """
                }
            }
        }

        stage('Check Status') {
            steps {
                script {
                    echo '🔍 Checking Status...'
                    sh '''
                        ssh -o StrictHostKeyChecking=no -i ${SSH_KEY_FILE} \\
                            ubuntu@${VM_IP} "sudo systemctl status moviebot --no-pager || true"
                        ssh -o StrictHostKeyChecking=no -i ${SSH_KEY_FILE} \\
                            ubuntu@${VM_IP} "sudo journalctl -u moviebot -n 20 --no-pager || true"
                    '''
                }
            }
        }
    }

    post {
        always {
            echo '🧹 Cleaning up...'
            script {
                def tfDir = 'infra/terraform'
                if (fileExists(tfDir)) {
                    dir("${tfDir}") {
                        withCredentials([
                            string(credentialsId: 'yc-iam-token', variable: 'TF_VAR_yc_token'),
                            string(credentialsId: 'yc-cloud-id', variable: 'TF_VAR_cloud_id'),
                            string(credentialsId: 'yc-folder-id', variable: 'TF_VAR_folder_id'),
                            string(credentialsId: 'ssh-public-key', variable: 'TF_VAR_ssh_public_key')
                        ]) {
                            sh '''
                                if [ -f "terraform.tfstate" ]; then
                                    terraform destroy -auto-approve -input=false \\
                                        -var="yc_token=${TF_VAR_yc_token}" \\
                                        -var="cloud_id=${TF_VAR_cloud_id}" \\
                                        -var="folder_id=${TF_VAR_folder_id}" \\
                                        -var="ssh_public_key=${TF_VAR_ssh_public_key}"
                                else
                                    echo "⚠️ No state file found."
                                fi
                            '''
                        }
                    }
                }
            }
            cleanWs()
        }
    }
}