pipeline {
	//Servers
	agent { label 'GITOPS'}
	
	//Parameters
	parameters {
		string ( name: 'NAMESPACE', description: 'Enter the Project name to be installed' )
                string ( name: 'REPO', description: 'Enter the repo name' )
	}

    environment { 
       http_proxy = 'http://myproxy.mydomain.com:80'
       https_proxy = 'http://myproxy.mydomain.com:80'
       HTTP_PROXY = 'http://myproxy.mydomain.com:80'
       HTTPS_PROXY = 'http://myproxy.mydomain.com:80'
       no_proxy = 'localhost,.mydomain.com.tr'
       NO_PROXY = 'localhost,.mydomain.com.tr'
    }

	stages{
	    stage('control') {
            steps {
                script {
                    if (!params.NAMESPACE.isEmpty()) { 
			if (params.REPO.isEmpty()) { 
                            error('repo Space is Empty') 
                        }
                    }
			else { error('Proje Space is Empty') }
                }
            }
        }
        stage('validate') {
            steps {
		script {

                        println "Environment is ${Ortam}"
                        if(Ortam == "DEV")
                            deploybranch = "dev"
                        else if(Ortam == "TEST")
                            deploybranch = "test"
                        else if(Ortam == "QA")
                            deploybranch = "qa"
                        else if(Ortam == "PROD")
                            deploybranch = "master"
                        else
                        error("Your DEPLOY environment is wrong, please check.")

                        repoUrl = "https://github.com/OktaySavdi/${params.REPO}.git"
                                               
                        git branch: deploybranch, credentialsId: 'srv_gitops', url: "${repoUrl}"
                        check_val = sh(script: "kubeval --openshift * | grep -v \"PASS\" | wc -l",returnStdout:true)
                        println check_val
                        if( check_val.toInteger() > 0 ){
                            sh "kubeval --openshift *"
                            error("Please check your yaml file")
                        }
                }
            }  
        }

		stage('action') {
			steps {
			    script {
		      def clusters = [
                        "https://api.mycluster1.mydomain:6443",
                        "https://api.mycluster2.mydomain:6443"
                        ]
                    for (int i=0; i < clusters.size(); ++i) {
                    def cluster = clusters[i]
                   
			git branch: deploybranch, credentialsId: 'srv_gitops', url: "${repoUrl}"
                     
			withCredentials([string(credentialsId: "ocpserviceuser_creds${i}", variable: 'TOKEN')]) {
			withCredentials([usernamePassword(credentialsId: 'srv_gitops', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
					
                        sh "oc login --token ${TOKEN} ${cluster} --insecure-skip-tls-verify=true"
						    
                            ARGOCD_SERVER_PASSWORD = sh(script: "oc get secret openshift-gitops-cluster -n openshift-gitops -o jsonpath='{.data.admin\\.password}' | base64 -d", returnStdout: true)
                            //println "ARGOCD_SERVER_PASSWORD=${ARGOCD_SERVER_PASSWORD}"
                            project_name = NAMESPACE.toLowerCase()
                            argocdrepourl = "https://${GIT_USERNAME}:${GIT_PASSWORD}@gtgit.mydomain.com.tr/scm/gitops/${params.REPO}.git"
            
                            score = sh(script: "polaris audit --config /opt/check_yaml_valiation/custom_check.yaml --audit-path . --format score", returnStdout: true).readLines()[0]

                            ARGOCD_ROUTE = sh(script:"oc -n openshift-gitops get route openshift-gitops-server -n openshift-gitops -o jsonpath='{.spec.host}'",returnStdout:true)
                            println ARGOCD_ROUTE
                            
                        sh """					
			    if [ \$(oc get project "${project_name}" | wc -l) -gt 0 ]; then
                                oc project $project_name
                            else
                              \"Project Not Found\"
                              exit 5
                            fi
                        """
                        
                            if(score.toInteger() < 80){
                                   println "Your yaml file has the following deficiencies"
                                   sh "polaris audit --config /opt/check_yaml_valiation/custom_check.yaml --audit-path . | grep -A 0 -B 8 false | grep -vE \"Severity|Category|Details|true\" "
                                   //kube-score score * --output-format ci | grep -vE "NetworkPolicy|podAntiAffinity|PodDisruptionBudget"
                                   error("Please make the relevant changes in your Yaml file")
                            }
                            else{   
                                argocdloginscript = """#!/bin/bash +x
                                argocd --insecure --grpc-web login ${ARGOCD_ROUTE}:443 --username admin --password ${ARGOCD_SERVER_PASSWORD}"""
                                
                                sh(script:argocdloginscript,returnStdout:false)
                                
                                projectcountscript = """#!/bin/bash
                                argocd app list | { grep -c ${project_name} || true; }
                                """
                                projectnumber = sh(script: projectcountscript,returnStdout:true).readLines()[0]
                                
                                
                                println "projectnumber is $projectnumber"
                                
                                if(projectnumber == "0"){
                                sh "argocd app create ${project_name} --repo ${argocdrepourl} --path . --revision ${deploybranch} --project default --dest-namespace ${project_name} --dest-server https://kubernetes.default.svc --directory-recurse"
                                }
                                
                                sh "argocd app sync ${project_name}"
                            }
						
		    }//cred
		}//withCredentials
             }//for
	  }//script
	}//steps
     }//stage
   }
}
