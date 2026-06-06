pipeline {

    agent any

    environment {

        FRONTEND_DOCKER_TAG = "${BUILD_NUMBER}"
        BACKEND_DOCKER_TAG = "${BUILD_NUMBER}"

        JWT_SECRET = credentials('jwt-secret')
    }

    stages {

        stage('Git Checkout') {

            steps {

                checkout([
                    $class: 'GitSCM',
                    branches: [[name: 'main']],
                    userRemoteConfigs: [[
                        url: 'https://github.com/Munaf/full-stack_chatApp.git',
                        credentialsId: 'Munaf_Git_Credentials'
                    ]],
                    extensions: [
                        [$class: 'CleanBeforeCheckout']
                    ]
                ])

                echo 'Git checkout completed successfully.'
            }
        }



        stage('Docker Build') {

            parallel {

                stage('Build Frontend Docker Image') {

                    steps {

                        dir('frontend') {

                            sh '''
                            docker build \
                              -t munafshaik/chatapp-frontend:$FRONTEND_DOCKER_TAG .
                            '''
                        }
                    }
                }



                stage('Build Backend Docker Image') {

                    steps {

                        dir('backend') {

                            sh '''
                            docker build \
                              -t munafshaik/chatapp-backend:$BACKEND_DOCKER_TAG .
                            '''
                        }
                    }
                }
            }
        }



        stage('Start Application Containers') {

            steps {

                sh '''
                docker compose up -d
                '''
            }
        }



        stage('Frontend Health Check') {

            steps {

                sh '''
                for i in {1..18}
                do

                  if curl -f http://localhost:3000 > /dev/null 2>&1
                  then
                    echo "Frontend application is UP"
                    exit 0
                  fi

                  echo "Waiting for frontend application..."
                  sleep 10

                done

                echo "Frontend application failed health check"
                exit 1
                '''
            }
        }



        stage('Backend Health Check') {

            steps {

                sh '''
                for i in {1..18}
                do

                  if curl -f http://localhost:5001 > /dev/null 2>&1
                  then
                    echo "Backend application is UP"
                    exit 0
                  fi

                  echo "Waiting for backend application..."
                  sleep 10

                done

                echo "Backend application failed health check"
                exit 1
                '''
            }
        }



        stage('Frontend DAST Scan') {

            steps {

                sh '''
                docker run --rm \
                  -v $(pwd):/zap/wrk/:rw \
                  ghcr.io/zaproxy/zaproxy:stable \
                  zap-baseline.py \
                  -t http://172.17.0.1:3000 \
                  -r frontend-dast-report.html \
                  -m 5
                '''
            }
        }



        stage('Backend DAST Scan') {

            steps {

                sh '''
                docker run --rm \
                  -v $(pwd):/zap/wrk/:rw \
                  ghcr.io/zaproxy/zaproxy:stable \
                  zap-baseline.py \
                  -t http://172.17.0.1:5001 \
                  -r backend-dast-report.html \
                  -m 5
                '''
            }
        }



        stage('Docker Login') {

            steps {

                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {

                    sh '''
                    echo $DOCKER_PASS | docker login \
                      -u $DOCKER_USER \
                      --password-stdin
                    '''
                }
            }
        }



        stage('Docker Push') {

            parallel {

                stage('Push Frontend Image') {

                    steps {

                        sh '''
                        docker push \
                          munafshaik/chatapp-frontend:$FRONTEND_DOCKER_TAG
                        '''
                    }
                }



                stage('Push Backend Image') {

                    steps {

                        sh '''
                        docker push \
                          munafshaik/chatapp-backend:$BACKEND_DOCKER_TAG
                        '''
                    }
                }
            }
        }
    }



    post {

        always {

            archiveArtifacts(
                artifacts: '**/*dast-report*.*',
                fingerprint: true,
                allowEmptyArchive: true
            )

            sh '''
            docker compose down -v || true
            '''

            sh '''
            docker logout || true
            '''
        }



        success {

            echo 'Pipeline completed successfully.'
        }



        failure {

            echo 'Pipeline failed.'
        }
    }
}