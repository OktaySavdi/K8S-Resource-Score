# K8S-Resource-Score

Requirments:

 - ArgoCD
 - Git
 - Jenkins
 - Openshift

A common file is created and the following config is added there
```bash
mkdir /opt/check_yaml_valiation
```
```bash
cat > /opt/check_yaml_valiation/custom_check.yaml << EOF
checks:
  # reliability
  multipleReplicasForDeployment: warning
  priorityClassNotSet: ignore
  tagNotSpecified: danger
  pullPolicyNotAlways: danger
  readinessProbeMissing: danger
  livenessProbeMissing: danger
  metadataAndNameMismatched: ignore
  pdbDisruptionsIsZero: ignore
  missingPodDisruptionBudget: ignore

  # efficiency
  cpuRequestsMissing: danger
  cpuLimitsMissing: danger
  memoryRequestsMissing: danger
  memoryLimitsMissing: danger
  # security
  hostIPCSet: ignore
  hostPIDSet: ignore
  notReadOnlyRootFilesystem: ignore
  privilegeEscalationAllowed: ignore
  runAsRootAllowed: ignore
  runAsPrivileged: ignore
  dangerousCapabilities: ignore
  insecureCapabilities: ignore
  hostNetworkSet: ignore
  hostPortSet: ignore
  tlsSettingsMissing: ignore
EOF
```
argocd, kubes-score and polaris are downloaded and left to the paths jenkins sees
```bash
wget https://github.com/zegl/kube-score/releases/download/v1.11.0/kube-score_1.11.0_linux_amd64.tar.gz
tar -xvf kube-score_1.11.0_linux_amd64.tar.gz
```
```bash
VERSION=$(curl --silent "https://api.github.com/repos/argoproj/argo-cd/releases/latest" | grep '"tag_name"' | sed -E 's/.*"([^"]+)".*/\1/')
curl -sSL -o /usr/local/bin/argocd https://github.com/argoproj/argo-cd/releases/download/$VERSION/argocd-linux-amd64
chmod +x /usr/local/bin/argocd
```
```bash
wget https://github.com/FairwindsOps/polaris/releases/download/4.0.2/polaris_4.0.2_linux_amd64.tar.gz
tar -xvf polaris_4.0.2_linux_amd64.tar.gz
```
```bash
cp /opt/{kube-score,polaris} /usr/bin/
```
jenkins declarative pipeline is imported ([Jenkinsfile](https://github.com/OktaySavdi/K8S-Resource-Score/blob/main/Jenkinsfile "Jenkinsfile"))

if yaml file score is below 80 you will get the following warning

![image](https://user-images.githubusercontent.com/3519706/121813079-36586480-cc73-11eb-8c34-dba5fc5a920a.png)

![image](https://user-images.githubusercontent.com/3519706/121812982-c3e78480-cc72-11eb-8c1d-cf2c2b8ae317.png)

if yaml file is over 80

![image](https://user-images.githubusercontent.com/3519706/121812923-8b47ab00-cc72-11eb-803a-5d5416cb5ac6.png)

![image](https://user-images.githubusercontent.com/3519706/121813038-04470280-cc73-11eb-8a6f-2140ce5188c7.png)

![image](https://user-images.githubusercontent.com/3519706/121813051-10cb5b00-cc73-11eb-8f10-bccba457d7fb.png)

![image](https://user-images.githubusercontent.com/3519706/121813978-4c682400-cc77-11eb-8949-10ae08732650.png)

### kube-score
```bash
kube-score score base-valid.yaml
kube-score score yaml/*
kube-score score yaml/* --output-format ci
kube-score score goodapp/* --output-format ci | grep -vE "NetworkPolicy|podAntiAffinity|PodDisruptionBudget"
```
### Polaris
```ruby
polaris audit --audit-path base-valid.yaml
polaris audit --audit-path test-data/base-valid.yaml --format score
polaris audit --audit-path yaml/ --format score
polaris audit --config config.yaml --audit-path yaml/
```
