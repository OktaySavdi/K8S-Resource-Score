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
cp kube-score /usr/bin/
cp kube-score /usr/local/sbin/
cp kube-score /usr/local/bin/
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
cp polaris /usr/bin/
cp polaris /usr/local/sbin/
cp polaris /usr/local/bin/
```
