### 쿠버네티스 설정 및 mysql 설치 (최초 1회만)
```
kubectl create namespace msa
kubectl get ns

mkdir ~/test/mysql
```

## Step 2. K8s에 MySQL 배포 (Local Path PV + PVC + Deployment + Service)
목표: ~/test/mysql을 PV로 연결 → PVC로 바인딩 → MySQL에 마운트
### k8s mysql.yaml 실행
```
kubectl apply -f run_k8s/mysql.yaml
kubectl get pods -w
kubectl get svc -n msa
kubectl get pvc,pv -n msa
```

## Step 3. 쿠버네티스의 mysql에 접속하기 위한 포트 포워드 실행
```
run_k8s/01_run_mysql_portforward.sh
```
Pod가 Running이 되면 OK
PVC가 Bound인지 확인

## Step 4. MSA 컴파일 및 쿠버네티스 배포 
