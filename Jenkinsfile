#!groovy
pipeline {

  agent any

  environment {
      BRANCH_NAME=env.GIT_BRANCH.replace("origin/", "")
  }

  stages {
    
    stage('Git Clone') {
      steps {
        script {
          sh "git clone https://github.com/Dashrath18/cdc-with-pact.git"
        }
      }
    }

    stage('Build') {
      steps {
        dir('messaging-app') {
           sh './mvnw clean verify'
        }
      }
    }

    stage('Publish Pacts') {
      steps {
        dir('messaging-app') {
           sh './mvnw pact:publish -Dpact.consumer.version=${GIT_COMMIT} -Dpact.tag=${BRANCH_NAME}'
        }
      }
    }

    stage('Check Pact Verifications') {
      steps {
        sh 'curl -LO https://github.com/pact-foundation/pact-ruby-standalone/releases/download/v1.61.1/pact-1.61.1-linux-x86_64.tar.gz'
        sh 'tar xzf pact-1.61.1-linux-x86_64.tar.gz'
        dir('pact/bin') {
          sh "./pact-broker can-i-deploy --retry-while-unknown=12 --retry-interval=10 -a messaging-app -b http://pact-broker-prod.pact-broker.svc.cluster.local:80 -e ${GIT_COMMIT}"
        }
      }
    }
    
    stage('Deploy') {
      when {
        branch 'master'
      }
      steps {
        echo 'Deploying to prod now...'
      }
    }

    stage ('Build') {
      steps {
        dir('user-service') {
          sh "./mvnw clean verify"
        }
      }
    }

    stage('Check Pact Verifications') {
      steps {
        sh 'curl -LO https://github.com/pact-foundation/pact-ruby-standalone/releases/download/v1.61.1/pact-1.61.1-linux-x86_64.tar.gz'
        sh 'tar xzf pact-1.61.1-linux-x86_64.tar.gz'
        dir('pact/bin') {
          sh "./pact-broker can-i-deploy -a user-service -b http://pact-broker-prod.pact-broker.svc.cluster.local:80 -e ${GIT_COMMIT}"
        }
      }
    }

    stage('Deploy') {
      when {
        branch 'master'
      }
      steps {
        echo 'Deploying to prod now...'
      }
    }
  }

}