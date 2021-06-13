# K8S-Resource-Score


### kube-score
```ruby
kube-score score base-valid.yaml
kube-score score yaml/*
kube-score score yaml/* --output-format ci
kube-score score goodapp/* --output-format ci | grep -vE "NetworkPolicy|podAntiAffinity|PodDisruptionBudget"
```
**Install**
```ruby
wget https://github.com/zegl/kube-score/releases/download/v1.11.0/kube-score_1.11.0_linux_amd64.tar.gz
tar -xvf kube-score_1.11.0_linux_amd64.tar.gz
```

### Polaris
```ruby
polaris audit --audit-path base-valid.yaml
polaris audit --audit-path test-data/base-valid.yaml --format score
polaris audit --audit-path yaml/ --format score
polaris audit --config config.yaml --audit-path yaml/
```
**Install**
```ruby
wget https://github.com/FairwindsOps/polaris/releases/download/4.0.2/polaris_4.0.2_linux_amd64.tar.gz
tar -xvf polaris_4.0.2_linux_amd64.tar.gz
```

### ArgoCD
```ruby
VERSION=$(curl --silent "https://api.github.com/repos/argoproj/argo-cd/releases/latest" | grep '"tag_name"' | sed -E 's/.*"([^"]+)".*/\1/')
curl -sSL -o /usr/local/bin/argocd https://github.com/argoproj/argo-cd/releases/download/$VERSION/argocd-linux-amd64
chmod +x /usr/local/bin/argocd
```



