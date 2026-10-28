pipeline {
    agent any
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Install Playwright Chromium') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'mvn -B org.codehaus.mojo:exec-maven-plugin:3.5.0:java "-Dexec.mainClass=com.microsoft.playwright.CLI" "-Dexec.args=install chromium"'
                    } else {
                        bat 'mvn -B org.codehaus.mojo:exec-maven-plugin:3.5.0:java "-Dexec.mainClass=com.microsoft.playwright.CLI" "-Dexec.args=install chromium"'
                    }
                }
            }
        }

        stage('Run UI tests') {
            steps {
                withCredentials([
                    string(credentialsId: 'jira-email', variable: 'JIRA_EMAIL'),
                    string(credentialsId: 'jira-api-token', variable: 'JIRA_API_TOKEN')
                ]) {
                    script {
                        if (isUnix()) {
                            sh 'mvn -B clean test -Dbrowser.headless=true'
                        } else {
                            bat 'mvn -B clean test -Dbrowser.headless=true'
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            junit testResults: 'target/surefire-reports/TEST-*.xml',
                    allowEmptyResults: true
            archiveArtifacts artifacts: 'target/surefire-reports/**,target/screenshots/**,target/extent-reports/**',
                    allowEmptyArchive: true
        }
    }
}
