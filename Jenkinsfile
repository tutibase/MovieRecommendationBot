pipeline {
    agent any

    environment {
        // === Credentials из Jenkins ===
        YC_TOKEN      = credentials('yc-iam-token')
        YC_CLOUD_ID   = credentials('yc-cloud-id')
        YC_FOLDER_ID  = credentials('yc-folder-id')
        SSH_KEY_FILE  = '/var/jenkins_home/.ssh/id_rsa'
        DB_PASSWORD   = credentials('db-password')
        ENV_FILE_PATH = credentials('bot-env-file')

        // === Пути в проекте ===
        TF_DIR        = 'infra/terraform'
        ANSIBLE_DIR   = 'infra/ansible'
        APP_DIR       = '/opt/moviebot'
        PROJECT_DIR   = 'MovieRecommendationBot'

        // === Переменные для пайплайна ===
        VM_IP         = ''
        JAR_FILE      = ''
    }

    stages {
        // ==========================================
        // ЭТАП 1: СБОРКА (BUILD)
        // ==========================================
        stage('Build Application') {
            steps {
                script {
                    echo '🚀 Starting Build Stage...'

                    sh 'docker rm -f build-db || true'

                    // 1. Поднимаем временный PostgreSQL
                    sh '''
                        docker run -d --name build-db \
                            -e POSTGRES_PASSWORD=${DB_PASSWORD} \
                            -e POSTGRES_DB=users_db \
                            -p 5432:5432 \
                            postgres:15

                        echo "⏳ Waiting for DB to start..."
                        sleep 10

                        cat ${PROJECT_DIR}/src/main/resources/users_db.sql | \
                            docker exec -i build-db psql -U postgres -d users_db
                    '''

                    // 2. Запускаем Maven сборку
                    dir("${PROJECT_DIR}") {
                        sh """
                            echo "Starting Maven Build with DB URL: host.docker.internal..."
                            mvn clean package -DskipTests \
                                -Ddb.password=${DB_PASSWORD} \
                                -Ddb.url=jdbc:postgresql://host.docker.internal:5432/users_db \
                                -Ddb.user=postgres
                        """
                    }

                    // 3. Находим собранный JAR
                    script {
                        def jars = findFiles(glob: "${PROJECT_DIR}/target/MovieRecommendationBot-*.jar")
                        JAR_FILE = jars.find { !it.name.contains('original') }?.path
                        if (!JAR_FILE) {
                            error "❌ JAR file not found!"
                        }
                        echo "✅ Found JAR: ${JAR_FILE}"
                    }

                    // 4. Чистим временную БД
                    sh 'docker rm -f build-db'
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: "${PROJECT_DIR}/target/*.jar", fingerprint: true
                }
            }
        }

        // ==========================================
        // ЭТАП 2: ИНФРАСТРУКТУРА (TERRAFORM)
        // ==========================================
        stage('Create Infrastructure') {
            steps {
                echo '☁️ Creating Infrastructure in Yandex Cloud...'
                dir("${TF_DIR}") {
                    withCredentials([
                        string(credentialsId: 'yc-iam-token', variable: 'TF_VAR_yc_token'),
                        string(credentialsId: 'yc-cloud-id', variable: 'TF_VAR_cloud_id'),
                        string(credentialsId: 'yc-folder-id', variable: 'TF_VAR_folder_id'),
                        string(credentialsId: 'ssh-public-key', variable: 'TF_VAR_ssh_public_key')
                    ]) {
                        sh '''
                            terraform init -input=false
                            terraform apply -auto-approve -input=false \
                                -var="yc_token=${TF_VAR_yc_token}" \
                                -var="cloud_id=${TF_VAR_cloud_id}" \
                                -var="folder_id=${TF_VAR_folder_id}" \
                                -var="ssh_public_key=${TF_VAR_ssh_public_key}"
                        '''
                    }
                }
            }
        }

        // ==========================================
        // ЭТАП 3: НАСТРОЙКА ОКРУЖЕНИЯ (ANSIBLE)
        // ==========================================
        stage('Provision Server') {
            steps {
                script {
                    echo '⚙️ Provisioning Server with Ansible...'

                    // 1. Получаем IP созданной ВМ
                    VM_IP = sh(script: "cd ${TF_DIR} && terraform output -raw vm_public_ip", returnStdout: true).trim()
                    echo "📍 VM IP: ${VM_IP}"

                    // 2. Генерируем Inventory для Ansible
                    sh """
                        echo "[all]" > ${ANSIBLE_DIR}/inventory.ini
                        echo "${VM_IP} ansible_user=ubuntu ansible_ssh_private_key_file=${SSH_KEY_FILE}" >> ${ANSIBLE_DIR}/inventory.ini
                    """

                    // 3. Запускаем Playbook
                    dir("${ANSIBLE_DIR}") {
                        sh """
                            ansible-playbook -i inventory.ini playbook.yml \
                                --private-key ${SSH_KEY_FILE} \
                                --extra-vars "db_password=${DB_PASSWORD} ansible_ssh_common_args='-o StrictHostKeyChecking=no'"
                        """
                    }
                }
            }
        }

        // ==========================================
        // ЭТАП 4: ДЕПЛОЙ АРТЕФАКТА
        // ==========================================
        stage('Deploy Artifact') {
            steps {
                script {
                    echo '📦 Deploying Application...'

                    if (!JAR_FILE) {
                        error "❌ JAR_FILE variable is empty. Build stage failed?"
                    }

                    // 1. Копируем JAR
                    sh """
                        scp -o StrictHostKeyChecking=no -i ${SSH_KEY_FILE} \
                            ${JAR_FILE} ubuntu@${VM_IP}:${APP_DIR}/MovieRecommendationBot.jar
                    """

                    // 2. Копируем .env файл
                    sh """
                        scp -o StrictHostKeyChecking=no -i ${SSH_KEY_FILE} \
                            ${ENV_FILE_PATH} ubuntu@${VM_IP}:${APP_DIR}/.env
                    """

                    // 3. Перезапускаем сервис
                    sh """
                        ssh -o StrictHostKeyChecking=no -i ${SSH_KEY_FILE} \
                            ubuntu@${VM_IP} "sudo systemctl daemon-reload && sudo systemctl restart moviebot"
                    """
                }
            }
        }

        // ==========================================
        // ЭТАП 5: ПРОВЕРКА СТАТУСА
        // ==========================================
        stage('Check Status') {
            steps {
                script {
                    echo '🔍 Checking Service Status...'
                    sh '''
                        ssh -o StrictHostKeyChecking=no -i ${SSH_KEY_FILE} \
                            ubuntu@${VM_IP} "sudo systemctl status moviebot --no-pager || true"

                        echo "📜 Last 20 logs:"
                        ssh -o StrictHostKeyChecking=no -i ${SSH_KEY_FILE} \
                            ubuntu@${VM_IP} "sudo journalctl -u moviebot -n 20 --no-pager || true"
                    '''
                }
            }
        }
    }

    // ==========================================
    // ФИНАЛ: ОЧИСТКА (ВСЕГДА)
    // ==========================================
    post {
        always {
            echo '🧹 Cleaning up infrastructure...'

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
                                    terraform destroy -auto-approve -input=false \
                                        -var="yc_token=${TF_VAR_yc_token}" \
                                        -var="cloud_id=${TF_VAR_cloud_id}" \
                                        -var="folder_id=${TF_VAR_folder_id}" \
                                        -var="ssh_public_key=${TF_VAR_ssh_public_key}"
                                else
                                    echo "⚠️ terraform.tfstate not found. Skipping destroy."
                                fi
                            '''
                        }
                    }
                } else {
                    echo "⚠️ Directory ${tfDir} not found. Skipping Terraform cleanup."
                }
            }

            cleanWs()
            echo '✅ Cleanup completed.'
        }
        failure {
            echo '❌ Pipeline failed! Check console output for details.'
        }
    }
}