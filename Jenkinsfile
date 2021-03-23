#!groovy
pipeline {

  agent {label "BusinessModule"}

  environment {
      BRANCH_NAME=env.GIT_BRANCH.replace("origin/", "")
  }

  parameters {
    string(defaultValue: "", description: 'Branch to build.', name: 'BRANCH_TO_BUILD')
  }



  stages {

    stage('Build') {
      when { 
        expression { retrun param.BRANCH_TO_BUILD == "DASH"}
      }
      steps {
        dir('messaging-app') {
           sh 'mvn clean verify -T 1C'
        }
      }
    }

    stage('Publish Pacts') {
       when { 
        expression { retrun param.BRANCH_TO_BUILD == "DASH"}
      }
      steps {
        dir('messaging-app') {
           sh 'mvn pact:publish -T 1C -Dpact.consumer.version=${GIT_COMMIT} -Dpact.tag=${BRANCH_NAME}'
        }
      }
    }

    stage ('Build User-Service') {
      when { 
        expression { retrun param.BRANCH_TO_BUILD == "MUND"}
      }
      steps {
        dir('user-service') {
          sh """ 
          mvn clean verify -T 1C \
          -Dpact.provider.version=${GIT_COMMIT} \
          -Dpact.verifier.publishResults=true
          """
        }
      }
    }

    stage('Check Pact Verifications') {
      when { 
        expression { retrun param.BRANCH_TO_BUILD == "MUND"}
      }
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

private branchToBuild() {
  if (BRANCH_TO_BUILD?.trim()) {
    return BRANCH_TO_BUILD
  }
  if (env.gitlabSourceBranch?.trim()) {
    return "origin/" + env.gitlabSourceBranch
  }
  return "origin/master"
}
