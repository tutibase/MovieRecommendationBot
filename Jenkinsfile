pipeline {
    agent any

    environment {
        // === Credentials из Jenkins ===
        // Важно: yc-oauth-token должен содержать OAuth токен (начинается на y0... или y1...)
        YC_OAUTH_TOKEN = credentials('yc-oauth-token')

        YC_CLOUD_ID    = credentials('yc-cloud-id')
        YC_FOLDER_ID   = credentials('yc-folder-id')

        DB_PASSWORD    = credentials('db-password')
        ENV_FILE_PATH  = credentials('bot-env-file')

        // Путь к приватному SSH ключу на сервере Jenkins
        SSH_KEY_FILE   = '/var/jenkins_home/.ssh/id_rsa'

        // === Пути в проекте ===
        TF_DIR         = 'MovieRecommendationBot/infra/terraform'
        ANSIBLE_DIR    = 'MovieRecommendationBot/infra/ansible'
        APP_DIR        = '/opt/moviebot'
        PROJECT_DIR    = 'MovieRecommendationBot'

        // === Переменные для пайплайна ===
        VM_IP          = ''
        JAR_FILE       = "${PROJECT_DIR}/target/MovieRecommendationBot-1.0-SNAPSHOT.jar"
    }

    stages {
        // ==========================================
        // ЭТАП 1: СБОРКА (BUILD)
        // ==========================================
        stage('Build Application') {
            steps {
                script {
                    echo '🚀 Starting Build Stage...'

                    // 0. Очистка старого контейнера БД (на всякий случай)
                    sh 'docker rm -f build-db || true'

                    // 1. Подъем БД и импорт схемы
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

                    // 2. ДИАГНОСТИКА (проверка порта)
                    echo '🔍 Testing TCP connection...'
                    sh '''
                        if timeout 5 bash -c 'echo > /dev/tcp/host.docker.internal/54321' 2>/dev/null; then
                            echo "✅ TCP Port 54321 is OPEN"
                        else
                            echo "❌ TCP Port 54321 is CLOSED or Unreachable"
                        fi
                    '''

                    // 3. Maven Сборка
                    dir("${PROJECT_DIR}") {
                        sh """
                            echo "🔨 Starting Maven Build..."
                            mvn clean package -DskipTests \\
                                -Ddb.password=${DB_PASSWORD} \\
                                -Ddb.url=jdbc:postgresql://host.docker.internal:54321/users_db \\
                                -Ddb.user=postgres
                        """
                    }

                    // 4. ПРОВЕРКА наличия JAR
                    script {
                        if (!fileExists(JAR_FILE)) {
                            error "❌ JAR file not found at ${JAR_FILE}!"
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

        // ==========================================
        // ЭТАП 2: ИНФРАСТРУКТУРА (TERRAFORM)
        // ==========================================
        stage('Create Infrastructure') {
            steps {
                echo '☁️ Creating Infrastructure in Yandex Cloud...'
                dir("${TF_DIR}") {
                    script {
                        // 1. Настраиваем зеркало Terraform
                        sh '''
                            mkdir -p ~/.terraform.d
                            cat > ~/.terraformrc <<EOF
provider_installation {
  network_mirror {
    url = "https://terraform-mirror.yandexcloud.net/"
    include = ["registry.terraform.io/*/*"]
  }
  direct {
    exclude = ["registry.terraform.io/*/*"]
  }
}
EOF
                            echo "✅ Terraform mirror configured"
                        '''

                        // 2. Аутентификация через yc CLI и получение свежего IAM токена
                        // Мы используем withCredentials только для OAuth, чтобы получить IAM динамически
                        withCredentials([string(credentialsId: 'yc-oauth-token', variable: 'YC_OAUTH')]) {
                            sh """
                                echo "🔑 Configuring yc CLI profile..."
                                # Настраиваем профиль по умолчанию
                                yc config set oauth ${YC_OAUTH}
                                yc config set cloud-id ${YC_CLOUD_ID}
                                yc config set folder-id ${YC_FOLDER_ID}

                                echo "🔄 Generating fresh IAM token..."
                                # Получаем IAM токен и сохраняем в переменную окружения Jenkins
                                export TF_VAR_yc_token=\$(yc iam create-token)

                                if [ -z "\$TF_VAR_yc_token" ]; then
                                    echo "❌ Failed to generate IAM token"
                                    exit 1
                                fi

                                echo "✅ IAM Token generated successfully."

                                # Инициализация и применение
                                terraform init -input=false

                                terraform apply -auto-approve -input=false \\
                                    -var="cloud_id=${YC_CLOUD_ID}" \\
                                    -var="folder_id=${YC_FOLDER_ID}" \\
                                    -var="ssh_public_key=\$(cat ~/.ssh/id_rsa.pub)"
                            """
                        }
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

                    if (VM_IP.isEmpty()) {
                        error "❌ Could not retrieve VM IP from Terraform state"
                    }

                    echo "📍 VM IP: ${VM_IP}"

                    // 2. Генерируем Inventory для Ansible
                    sh """
                        mkdir -p ${ANSIBLE_DIR}
                        echo "[all]" > ${ANSIBLE_DIR}/inventory.ini
                        echo "${VM_IP} ansible_user=ubuntu ansible_ssh_private_key_file=${SSH_KEY_FILE}" >> ${ANSIBLE_DIR}/inventory.ini
                    """

                    // 3. Запускаем Playbook
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

        // ==========================================
        // ЭТАП 4: ДЕПЛОЙ АРТЕФАКТА
        // ==========================================
        stage('Deploy Artifact') {
            steps {
                script {
                    echo '📦 Deploying Application...'

                    if (!fileExists(JAR_FILE)) {
                        error "❌ Cannot deploy: JAR file missing!"
                    }

                    // 1. Копируем JAR
                    sh """
                        scp -o StrictHostKeyChecking=no -i ${SSH_KEY_FILE} \\
                            ${JAR_FILE} ubuntu@${VM_IP}:${APP_DIR}/MovieRecommendationBot.jar
                    """

                    // 2. Копируем .env файл
                    sh """
                        scp -o StrictHostKeyChecking=no -i ${SSH_KEY_FILE} \\
                            ${ENV_FILE_PATH} ubuntu@${VM_IP}:${APP_DIR}/.env
                    """

                    // 3. Перезапускаем сервис
                    sh """
                        ssh -o StrictHostKeyChecking=no -i ${SSH_KEY_FILE} \\
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
                    sh """
                        ssh -o StrictHostKeyChecking=no -i ${SSH_KEY_FILE} \\
                            ubuntu@${VM_IP} "sudo systemctl status moviebot --no-pager || true"

                        echo "📜 Last 20 logs:"
                        ssh -o StrictHostKeyChecking=no -i ${SSH_KEY_FILE} \\
                            ubuntu@${VM_IP} "sudo journalctl -u moviebot -n 20 --no-pager || true"
                    """
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
                def tfDir = 'MovieRecommendationBot/infra/terraform'

                if (fileExists(tfDir)) {
                    dir("${tfDir}") {
                        // Повторяем логику аутентификации для destroy
                        withCredentials([string(credentialsId: 'yc-oauth-token', variable: 'YC_OAUTH')]) {
                            sh """
                                # Проверяем наличие стейта перед уничтожением
                                if [ -f "terraform.tfstate" ]; then
                                    echo "🔑 Re-authenticating for destroy..."
                                    yc config set oauth ${YC_OAUTH}
                                    yc config set cloud-id ${YC_CLOUD_ID}
                                    yc config set folder-id ${YC_FOLDER_ID}

                                    export TF_VAR_yc_token=\$(yc iam create-token)

                                    echo "🗑️ Destroying infrastructure..."
                                    terraform destroy -auto-approve -input=false \\
                                        -var="cloud_id=${YC_CLOUD_ID}" \\
                                        -var="folder_id=${YC_FOLDER_ID}" \\
                                        -var="ssh_public_key=\$(cat ~/.ssh/id_rsa.pub)"
                                else
                                    echo "⚠️ No state file found. Skipping destroy."
                                fi
                            """
                        }
                    }
                } else {
                    echo "⚠️ Directory ${tfDir} not found. Skipping Terraform cleanup."
                }
            }

            // Очистка рабочего пространства Jenkins
            cleanWs()
        }
        failure {
            echo '❌ Pipeline failed! Check console output for details.'
        }
    }
}