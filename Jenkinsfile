
pipeline {
    agent any

    environment {
        JIRA_BASE_URL    = credentials('jira-base-url')
        JIRA_EMAIL       = credentials('jira-email')
        JIRA_API_TOKEN   = credentials('jira-api-token')
        JIRA_PROJECT_KEY = credentials('jira-project-key')
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out automation framework from GitHub'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Compiling Java automation framework'
                bat 'mvn -B -DskipTests clean compile'
            }
        }

        stage('Run UI Tests') {
            steps {
                echo 'Executing Playwright TestNG tests'
                bat 'mvn -B test'
            }
        }
    }

    post {
        always {
            echo 'Publishing test reports and artifacts'

            // Publish TestNG / Maven Surefire results
            junit(
                testResults: 'target/surefire-reports/*.xml',
                allowEmptyResults: true
            )

            // Publish Extent HTML report
            publishHTML(target: [
                reportName: 'Extent Report',
                reportDir: 'target/extent-reports',
                reportFiles: 'ExtentReport.html',
                keepAll: true,
                alwaysLinkToLastBuild: true,
                allowMissing: true
            ])

            // Archive reports and screenshots
            archiveArtifacts(
                artifacts: 'target/extent-reports/**, target/surefire-reports/**, target/screenshots/**',
                allowEmptyArchive: true
            )
        }

        success {
            echo 'Playwright automation pipeline completed successfully.'
        }

        failure {
            echo 'Pipeline failed. Check Console Output, test reports, and Jira.'
        }

        cleanup {
            echo 'Pipeline execution finished.'
        }
    }
}
