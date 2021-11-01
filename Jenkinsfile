pipeline {
	//Servers
	agent any
	
	//Parameters
	parameters {
		string ( name: 'NAMESPACE', description: 'Enter the Project name to be installed' )
		string ( name: 'branch' , description: 'Enter the branch name'               )
	}
	
	stages{
	    stage('control') {
            steps {
                script {
                    if (!params.NAMESPACE.isEmpty()) { 
						if (params.branch.isEmpty()) { error('Branch Field Empty') }
                    }
					else { error('Project Area is Empty') }
                }
            }
        }
		stage('action') {
			steps {
				script {
					git branch:"${params.branch}", url:'https://github.com/OktaySavdi/K8S-Resource-Score.git'
					withCredentials([string(credentialsId: "ocp_creds", variable: 'TOKEN')]) {
						sh '''
		            				oc login --token ${TOKEN} https://api.mycluster.mydomain:6443 --insecure-skip-tls-verify=true
                            				ARGOCD_SERVER_PASSWORD=$(oc get secret openshift-gitops-cluster -n openshift-gitops -o jsonpath='{.data.admin\\.password}' | base64 -d)
                            				ARGOCD_ROUTE=$(oc -n openshift-gitops get route openshift-gitops-server -n openshift-gitops -o jsonpath='{.spec.host}')
                            				score=$(polaris audit --config /opt/check_yaml_valiation/custom_check.yaml --audit-path . --format score)										
							if [ $(oc get project "${NAMESPACE}" | wc -l) -gt 0 ]; then
                                			   oc project $NAMESPACE
                            				else
                            				    oc new-project ${NAMESPACE}
                            				fi
                            				if [[ $score -lt '80' ]];then
                            				       echo "Your YAML file has the following missing."
							       polaris audit --config /opt/check_yaml_valiation/custom_check.yaml --audit-path . | grep -A 0 -B 8 false | grep -vE "Severity|Category|Details|true"
                            				       #kube-score score * --output-format ci | grep -vE "NetworkPolicy|podAntiAffinity|PodDisruptionBudget"
                            				else                              
                            				    argocd --insecure --grpc-web login ${ARGOCD_ROUTE}:443 --username admin --password ${ARGOCD_SERVER_PASSWORD}
                            				    if [[ $(argocd app list | grep -c "${NAMESPACE}") -eq '0' ]];then
                            				        argocd app create "${NAMESPACE}" --repo https://github.com/OktaySavdi/K8S-Resource-Score.git --path . --revision "${branch}" --project default --dest-namespace "${NAMESPACE}" --dest-server https://api.mycluster.mydomain:6443 --directory-recurse
                            				    fi
                            				    argocd app sync "${NAMESPACE}"
                            				fi
						'''
					}
				}
			}
		}
	}
}
