@Library('Shared') _

pipeline {

    agent any

    environment {

        DOCKER_IMAGE_NAME = 'bdhanore26/easyshop-app'
        DOCKER_MIGRATION_IMAGE_NAME = 'bdhanore26/easyshop-migration'
        DOCKER_IMAGE_TAG = "${BUILD_NUMBER}"

        GITHUB_CREDENTIALS = credentials('github-credentials')

        GIT_BRANCH = "master"
    }

    stages {

        stage('Prevent Build Loop') {

            steps {

                script {

                    def commitMsg = sh(
                        script: "git log -1 --pretty=%B",
                        returnStdout: true
                    ).trim()

                    echo "Latest commit message: ${commitMsg}"

                    if (
                        commitMsg.contains("[skip ci]") ||
                        commitMsg.contains("Update image tags")
                    ) {

                        echo "Skipping Jenkins auto-generated commit build"

                        currentBuild.description =
                            "Skipped Jenkins-generated commit"

                        currentBuild.result = 'NOT_BUILT'

                        error("Stopping pipeline to prevent CI loop")
                    }
                }
            }
        }

        stage('Cleanup Workspace') {

            steps {

                script {

                    clean_ws()

                }
            }
        }

        stage('Clone Repository') {

            steps {

                script {

                    clone(
                        "https://github.com/bdhanore26/e-commerce-app.git",
                        "master"
                    )

                }
            }
        }

        stage('Build Docker Images') {

            parallel {

                stage('Build Main App Image') {

                    steps {

                        script {

                            docker_build(
                                imageName: env.DOCKER_IMAGE_NAME,
                                imageTag: env.DOCKER_IMAGE_TAG,
                                dockerfile: 'Dockerfile',
                                context: '.'
                            )

                        }
                    }
                }

                stage('Build Migration Image') {

                    steps {

                        script {

                            docker_build(
                                imageName: env.DOCKER_MIGRATION_IMAGE_NAME,
                                imageTag: env.DOCKER_IMAGE_TAG,
                                dockerfile: 'scripts/Dockerfile.migration',
                                context: '.'
                            )

                        }
                    }
                }
            }
        }

        stage('Run Unit Tests') {

            steps {

                script {

                    run_tests()

                }
            }
        }

        stage('Security Scan with Trivy') {

            steps {

                script {

                    trivy_scan()

                }
            }
        }

        stage('Push Docker Images') {

            parallel {

                stage('Push Main App Image') {

                    steps {

                        script {

                            docker_push(
                                imageName: env.DOCKER_IMAGE_NAME,
                                imageTag: env.DOCKER_IMAGE_TAG,
                                credentials: 'dockerhub-credentials'
                            )

                        }
                    }
                }

                stage('Push Migration Image') {

                    steps {

                        script {

                            docker_push(
                                imageName: env.DOCKER_MIGRATION_IMAGE_NAME,
                                imageTag: env.DOCKER_IMAGE_TAG,
                                credentials: 'dockerhub-credentials'
                            )

                        }
                    }
                }
            }
        }

        stage('Update Kubernetes Manifests') {

            steps {

                script {

                    update_k8s_manifests(
                        imageTag: env.DOCKER_IMAGE_TAG,
                        manifestsPath: 'kubernetes',
                        gitCredentials: 'github-credentials',
                        gitUserName: 'Jenkins CI',
                        gitUserEmail: 'bdhanore26@gmail.com'
                    )

                }
            }
        }
    }

    post {

        success {

            echo "Pipeline completed successfully"

        }

        failure {

            echo "Pipeline failed"

        }

        always {

            cleanWs()

        }
    }
}
