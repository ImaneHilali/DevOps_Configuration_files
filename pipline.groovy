        
pipeline {
    agent any 

    stages {
        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    userRemoteConfigs: [[
                        url: 'https://github.com/salaheddinerossi/announcementservice.git',
                    ]]
                ])
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    def tag = "${env.BUILD_NUMBER}" // Using Jenkins build number for the tag
                    def imageName = "salaheddinerossi/announcementservice:${tag}"
                    sh "docker build -t ${imageName} ."
                }
            }
        }
        
        stage('Push Docker Image to Registry') {
            steps {
                script {
                    def tag = "${env.BUILD_NUMBER}" // Using Jenkins build number for the tag
                    def imageName = "salaheddinerossi/announcementservice:${tag}"
                    withCredentials([usernamePassword(credentialsId: 'docker', passwordVariable: 'DOCKERHUB_PASS', usernameVariable: 'DOCKERHUB_USER')]) {
                        sh "docker login -u ${DOCKERHUB_USER} -p ${DOCKERHUB_PASS}"
                        sh "docker push ${imageName}"
                    }
                }
            }
        }
        
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    withCredentials([file(credentialsId: 'k8', variable: 'KUBECONFIG')]) {
                        sh "kubectl set image deployment/announcementservice-deployment announcementservice=salaheddinerossi/announcementservice:${env.BUILD_NUMBER} --kubeconfig=${KUBECONFIG}"
                    }
                }
            }
        }

    }
}
