pipeline {
	//Servers
	agent { label 'GITOPS'}
	
	//Parameters
	parameters {
	choice ( name: 'CLUSTERS', description: 'Enter the Cluster name to be installed', choices: ['my_app_cluster', 'my_web_cluster', 'my_db_cluster'] )
        string ( name: 'NAMESPACE', description: 'Enter the Project name to be installed' )
        string ( name: 'REPO', description: 'Enter the repo name' )
        choice ( name: 'Deploy', description: 'If it works in an active-passive structure, which cluster should be deployed?? ', choices: ['all', 'cluster_1', 'cluster_2'] )
	}

    environment { 
       my_app_cluster = "https://api.myappcluster1.mydomain:644, https://api.myappcluster2.mydomain:644"
       my_web_cluster = "https://api.mywebcluster1.mydomain:644, https://api.mywebcluster1.mydomain:644"
       my_db_cluster = "https://api.mydbcluster1.mydomain:644, https://api.mydbcluster1.mydomain:644"
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

                        deploybranch = sh( script: "echo ${NAMESPACE} | awk -F '-' '{print \$NF}' | tr '[:upper:]' '[:lower:]'",returnStdout:true).trim()
                        if(deploybranch == "dev")
                            deploybranch = "dev"
                        else if(deploybranch == "test")
                            deploybranch = "test"
                        else if(deploybranch == "qa")
                            deploybranch = "qa"
                        else if(deploybranch == "PROD")
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
                        }//if
                }//script
            }//steps  
        }//stage

		stage('action') {
			steps {
				script {
                                     env."${CLUSTERS}".tokenize(",").each { cluster ->
 				     git branch: deploybranch, credentialsId: 'srv_gitops', url: "${repoUrl}"
                                     cred_id = sh ( script: "echo ${cluster} | sed 's|https://\\(api\\.\\)\\?\\(.*\\)|\\2|' | awk -F '.' '{print \$1}' | tr '-' '_' | awk '{print \$0 \"_gitops\"}'",returnStdout:true)
				     withCredentials([string(credentialsId: "${cred_id}", variable: 'TOKEN')]) {
				     withCredentials([usernamePassword(credentialsId: 'srv_gitops', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
					
                                  sh "oc login --token ${TOKEN} ${cluster} --insecure-skip-tls-verify=true"
						    
                                     ARGOCD_SERVER_PASSWORD = sh(script: "oc get secret openshift-gitops-cluster -n openshift-gitops -o jsonpath='{.data.admin\\.password}' | base64 -d", returnStdout: true)
                                     project_name = NAMESPACE.toLowerCase()
                                     argocdrepourl = "https://${GIT_USERNAME}:${GIT_PASSWORD}@gtgit.mydomain.com.tr/scm/gitops/${params.REPO}.git"
            
                                     score = sh(script: "polaris audit --config /opt/check_yaml_valiation/custom_check.yaml --audit-path . --format score", returnStdout: true).readLines()[0]

                                     ARGOCD_ROUTE = sh(script:"oc -n openshift-gitops get route openshift-gitops-server -n openshift-gitops -o jsonpath='{.spec.host}'",returnStdout:true)                           
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
                                         if ( Deploy != 'all') 
                                         {
                                            if ( Deploy == 'cluster_1' && cluster =~ /(.*)1\.fw(.*)/ )
                                            {
                                              sh "argocd app sync ${project_name}"
                                            }else if ( Deploy == 'cluster_2' && cluster =~ /(.*)2\.fw(.*)/)
                                               {sh "argocd app sync ${project_name}"}
                                         }
                                         else{
                                               sh "argocd app sync ${project_name}"
                                         }//else
                                         
                                     }						
				 }//cred
			      }//withCredentials
                           }//each
			}//script
		      }//steps
		  }//stage
		}
		
		post {
        success {
            emailext (
                              subject: "GITOPS DEPLOYMENT SUCCESS - ${SmartID}" ,
                              body: """<span style="font-family: Arial; font-size: 10pt">
                              <p>Check console output at <a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a></p> </span>""",
                              to : "${Email}",
                              attachLog: true
            )

        }
        failure {
            emailext (
                              subject: "GITOPS DEPLOYMENT FAILED - ${SmartID}" ,
                              body: """<span style="font-family: Arial; font-size: 10pt">
                              <p>Check console output at <a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a></p> </span>""",
                              to : "${Email}",
                              attachLog: true
            )

        }
    }   
  
}
