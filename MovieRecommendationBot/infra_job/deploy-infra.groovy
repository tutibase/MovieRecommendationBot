pipeline {
    agent { label 'poly-agent' }

    options {
        // Запрещаем параллельные сборки
        disableConcurrentBuilds()
    }

    parameters {
        // Опциональная принудительная очистка перед деплоем
        booleanParam(name: 'FORCE_CLEANUP', defaultValue: false,
                description: 'Удалить существующий стек перед созданием нового')
    }

    environment {
        STACK_NAME = "movie-bot-infra-${env.BUILD_NUMBER}"
        HEAT_TEMPLATE = 'heat/stack.yaml'
    }

    stages {

        // Тест аутентификации в OpenStack
        stage('Test OpenStack Connection') {
            steps {
                script {
                    echo "🔑 Testing OpenStack authentication..."
                    withCredentials([file(credentialsId: 'openstack-rc-file',
                            variable: 'OPENSTACK_RC')]) {
                        sh '''
                            set +x  # Не логировать чувствительные данные
                            . $OPENSTACK_RC
                            
                            echo "📡 Checking connection to OpenStack..."
                            openstack token issue -f yaml
                            
                            if [ $? -eq 0 ]; then
                                echo "✅ Authentication successful!"
                            else
                                echo "❌ Authentication failed!"
                                exit 1
                            fi
                        '''
                    }
                }
            }
        }

        // Проверка и очистка существующего стека
        stage('Check & Cleanup Existing Stack') {
            steps {
                script {
                    withCredentials([file(credentialsId: 'openstack-rc-file', variable: 'OPENSTACK_RC')]) {
                        def stackStatus = sh(
                                script: '''
                        set +x
                        . $OPENSTACK_RC
                        openstack stack show ${STACK_NAME} -f value -c stack_status 2>/dev/null || echo "NOT_FOUND"
                    '''.stripIndent(),
                                returnStdout: true
                        ).trim()

                        if (stackStatus != 'NOT_FOUND') {
                            echo "⚠️ Stack exists with status: ${stackStatus}. Cleaning up..."
                            sh '''
                        set +x
                        . $OPENSTACK_RC
                        openstack stack delete --yes ${STACK_NAME}
                    '''

                            echo "⏳ Waiting for deletion..."
                            timeout(time: 5, unit: 'MINUTES') {
                                sh '''
                            while openstack stack show ${STACK_NAME} -f value -c stack_status 2>/dev/null | grep -q .; do
                                sleep 5
                            done
                        '''
                            }
                            echo "✅ Stack deleted"
                        } else {
                            echo "✅ No existing stack found. Proceeding with creation."
                        }
                    }
                }
            }
        }

        stage('Validate Template') {
            steps {
                withCredentials([file(credentialsId: 'openstack-rc-file',
                        variable: 'OPENSTACK_RC')]) {
                    sh '''
                        set +x
                        . $OPENSTACK_RC
                        echo "🔍 Validating Heat template..."
                        openstack stack template validate -t ${HEAT_TEMPLATE}
                    '''
                }
            }
        }

        stage('Deploy Stack') {
            steps {
                script {
                    echo "🚀 Creating stack: ${STACK_NAME}"
                    withCredentials([file(credentialsId: 'openstack-rc-file',
                            variable: 'OPENSTACK_RC')]) {
                        //Используем флаг --wait для ожидания завершения
                        sh '''
                            set +x
                            source $OPENSTACK_RC
                            
                            echo "Deploying infrastructure..."
                            openstack stack create \
                                -t ${HEAT_TEMPLATE} \
                                --parameter key_name=lugov-key-pair \
                                --parameter image_id=ubuntu-22.04 \
                                --parameter flavor_id=m1.small \
                                --parameter existing_subnet_id=d80da048-c188-45a5-80e4-55d914fe58ea \
                                --wait \
                                ${STACK_NAME}
                            
                            echo "✅ Stack creation completed!"
                        '''
                    }
                }
            }
        }

        stage('Get Outputs') {
            steps {
                script {
                    withCredentials([file(credentialsId: 'openstack-rc-file',
                            variable: 'OPENSTACK_RC')]) {
                        def output = sh(
                                script: "source \$OPENSTACK_RC && openstack stack output show -c output_value -f value ${STACK_NAME} server_private_ip",
                                returnStdout: true
                        ).trim()
                        echo "🌐 Server IP: ${output}"
                        env.SERVER_IP = output

                        // Сохраняем все выводы в файл для архивации
                        sh '''
                            source $OPENSTACK_RC
                            openstack stack output show --all --format json ${STACK_NAME} > stack_outputs.json
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            // 📦 Архивируем артефакты (даже если сборка упала)
            archiveArtifacts artifacts: 'stack_outputs.json', allowEmptyArchive: true

            // 🧹 Очищаем рабочую директорию
            cleanWs()
        }

        // Обработка падения — удаляем стек, чтобы не осталось "мусора"
        failure {
            echo "Pipeline failed! Cleaning up..."
            withCredentials([file(credentialsId: 'openstack-rc-file',
                    variable: 'OPENSTACK_RC')]) {
                sh '''
                    set +x
                    source $OPENSTACK_RC
                    
                    # Проверяем, существует ли стек, перед удалением
                    if openstack stack show ${STACK_NAME} &>/dev/null; then
                        echo "🗑️ Deleting failed stack ${STACK_NAME}..."
                        openstack stack delete --yes ${STACK_NAME} || true
                        echo "✅ Cleanup completed"
                    else
                        echo "ℹ️ Stack ${STACK_NAME} does not exist, nothing to clean"
                    fi
                '''
            }
            echo "Check OpenStack dashboard or run: openstack stack event list ${STACK_NAME}"
        }

        success {
            echo "Stack deployed successfully!"
            echo "Server IP: ${SERVER_IP}"
            echo "SSH: ssh -i <key> ubuntu@${SERVER_IP}"
            echo "Outputs saved to: stack_outputs.json"
        }
    }
}