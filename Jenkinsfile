#!groovy
pipeline {

  agent {label "BusinessModule"}

  environment {
      BRANCH_NAME=env.GIT_BRANCH.replace("origin/", "")
  }

  stages {

    stage('Build') {
      steps {
        dir('messaging-app') {
           sh 'mvn clean verify'
        }
      }
    }

    stage('Publish Pacts') {
      steps {
        dir('messaging-app') {
           sh 'mvn pact:publish -Dpact.consumer.version=${GIT_COMMIT} -Dpact.tag=${BRANCH_NAME}'
        }
      }
    }

    stage ('Build User-Service') {
      steps {
        dir('user-service') {
          sh """ 
          mvn clean verify \
          -Dpact.provider.version=${GIT_COMMIT} \
          -Dpact.verifier.publishResults=true
          """
        }
      }
    }
    
    stage('Check Pact Verifications messaging-app') {
      steps {
        dir('messaging-app'){
          sh 'curl -LO https://github.com/pact-foundation/pact-ruby-standalone/releases/download/v1.61.1/pact-1.61.1-linux-x86_64.tar.gz'
          sh 'tar xzf pact-1.61.1-linux-x86_64.tar.gz'
          dir('pact/bin') {
            sh "./pact-broker can-i-deploy --retry-while-unknown=12 --retry-interval=10 -a messaging-app -b http://pact-broker-prod.pact-broker.svc.cluster.local:80 -e ${GIT_COMMIT}"
          }
        }
      }
    }

    stage('Check Pact Verifications') {
      steps {
        dir('user-service') {
          sh 'curl -LO https://github.com/pact-foundation/pact-ruby-standalone/releases/download/v1.61.1/pact-1.61.1-linux-x86_64.tar.gz'
          sh 'tar xzf pact-1.61.1-linux-x86_64.tar.gz'
          dir('pact/bin') {
            sh "./pact-broker can-i-deploy -a user-service -b http://pact-broker-prod.pact-broker.svc.cluster.local:80 -e ${GIT_COMMIT}"
          }
        }
      }
    }

    stage('Deploy user Service') {
      when {
        branch 'master'
      }
      steps {
        echo 'Deploying to prod now...'
      }
    }
  }

}