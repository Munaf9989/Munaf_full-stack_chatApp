pipeline {
    agent any

    environment {
        SCANNER_HOME = tool 'sonar-scanner'
    }

    parameters {
        string(name: 'FRONTEND_DOCKER_TAG', defaultValue: '', description: 'Setting docker image for latest push')
        string(name: 'BACKEND_DOCKER_TAG', defaultValue: '', description: 'Setting docker image for latest push')
    }
    
    stages {
        stage("Validate Parameters") {
            steps {
                script {
                    if (params.FRONTEND_DOCKER_TAG == '' || params.BACKEND_DOCKER_TAG == '') {
                        error("FRONTEND_DOCKER_TAG and BACKEND_DOCKER_TAG must be provided.")
                    }
                }
            }
        }
        stage('Git_SCM_checkout') {
            steps {
                checkout([$class: 'GitSCM',
                            branches: [[name: 'main']],
                            userRemoteConfigs: [[url: 'https://github.com/Munaf9989/Munaf_full-stack_chatApp.git', credentialsId: 'Munaf_Git_Credentials']],
                              extensions: [
                                [$class: 'CleanBeforeCheckout']
                            ]])
                echo 'Git checkout completed successfully.'
            }
            post {
                failure {
                    echo 'Git checkout failed.'
                }
                success {
                    echo 'Git checkout succeeded.'
                }
            }
        }
        stage('Trivy scan') {
            parallel {
                stage('Frontend Trivy Scan') {
                    steps {
                        sh '''
                        trivy fs frontend \
                        --severity HIGH,CRITICAL \
                        --format table \
                        --output frontend-trivy-report.txt \
                         #--exit-code 1 \
                        --no-progress
                        '''
                    }
                }

                stage('Backend Trivy Scan') {
                    steps {
                        sh '''
                        trivy fs backend \
                        --severity HIGH,CRITICAL \
                        --format table \
                        --output backend-trivy-report.txt \
                         #--exit-code 1 \
                        --no-progress
                        '''
                    }
                }
            }
        }

        stage('SonarQube Analysis') {

            parallel {

                stage('Frontend Sonar Scan') {
                    steps {
                        withSonarQubeEnv('sonar-server') {
                            dir('frontend') {

                                sh '''
                                ${SCANNER_HOME}/bin/sonar-scanner \
                                  -Dsonar.projectKey=frontend-chatapp \
                                  -Dsonar.sources=. \
                                '''
                            }
                        }
                    }
                }
                stage('Backend Sonar Scan') {
                    steps {
                        withSonarQubeEnv('sonar-server') {

                            dir('backend') {
                                sh '''
                                ${SCANNER_HOME}/bin/sonar-scanner \
                                  -Dsonar.projectKey=backend-chatapp \
                                  -Dsonar.sources=. \
                                '''
                            }
                        }
                    }
                }
            }
        }
        stage('Quality Gates') {

            parallel {

                stage('Frontend Quality Gate') {
                    steps {
                        timeout(time: 5, unit: 'MINUTES') {
                            waitForQualityGate abortPipeline: true
                        }
                    }
                }

                stage('Backend Quality Gate') {
                    steps {
                        timeout(time: 5, unit: 'MINUTES') {
                            waitForQualityGate abortPipeline: true
                        }
                    }
                }
            }
        }
        stage('Docker_Build') {
            parallel {
                stage('Build Frontend Docker Image') {
                    steps{
                        dir('frontend') {
                            echo 'Building Frontend Docker image...'
                            sh '''
                            docker build -t munafshaik/chatapp-frontend:$FRONTEND_DOCKER_TAG .
                            '''
                            echo 'Frontend Docker image built successfully.'
                        }
                    }
                }
                stage('Build Backend Docker Image') {
                    steps{
                        dir('backend') {
                            echo 'Building Backend Docker image...'
                            sh '''
                            docker build -t munafshaik/chatapp-backend:$BACKEND_DOCKER_TAG .
                            '''
                            echo 'Backend Docker image built successfully.'
                            }
                        }
                    }
            }
        }
        stage('Parallel Trivy Image Scan') {

            parallel {

                stage('Frontend Image Scan') {
                    steps {

                        sh '''
                        trivy image \
                        --scanners vuln \
                        --severity HIGH,CRITICAL \
                        --ignore-unfixed \
                        --format table \
                        --output frontend-image-scan-report.txt \
                        --no-progress \
                        munafshaik/chatapp-frontend:$FRONTEND_DOCKER_TAG
                        '''
                    }
                }
                stage('Backend Image Scan') {
                    steps {

                        sh '''
                        trivy image \
                        --scanners vuln \
                        --severity HIGH,CRITICAL \
                        --ignore-unfixed \
                        --format table \
                        --output backend-image-scan-report.txt \
                        --no-progress \
                        munafshaik/chatapp-backend:$BACKEND_DOCKER_TAG
                        '''
                    }
                }
            }
        }
        stage('Docker Push') {

            parallel {

                stage('Push Frontend Image') {
                    steps {

                        withCredentials([usernamePassword(
                            credentialsId: 'dockerhub-creds',
                            usernameVariable: 'DOCKER_USER',
                            passwordVariable: 'DOCKER_PASS'
                        )]) {

                            sh '''
                            echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin

                            docker push munafshaik/chatapp-frontend:$FRONTEND_DOCKER_TAG
                            '''
                        }
                    }
                }

                stage('Push Backend Image') {
                    steps {

                        withCredentials([usernamePassword(
                            credentialsId: 'dockerhub-creds',
                            usernameVariable: 'DOCKER_USER',
                            passwordVariable: 'DOCKER_PASS'
                        )]) {

                            sh '''
                            echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin

                            docker push munafshaik/chatapp-backend:$BACKEND_DOCKER_TAG
                            '''
                        }
                    }
                }
            }
        }

    }

    post {
        always {
            archiveArtifacts artifacts: '*report.txt', fingerprint: true
        }
        success {
            build job: 'Chat-App-CD',
            parameters: [
                string(name: 'FRONTEND_DOCKER_TAG', value: "${params.FRONTEND_DOCKER_TAG}"),
                string(name: 'BACKEND_DOCKER_TAG', value: "${params.BACKEND_DOCKER_TAG}")
            ]
            }
    }
}
