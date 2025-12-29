# 쿠버네티스 설정 및 mysql 설치
```
kubectl create namespace msa
kubectl get ns

mkdir ~/test/mysql
```

## Step 2. K8s에 MySQL 배포 (Local Path PV + PVC + Deployment + Service)

목표: ~/test/mysql을 PV로 연결 → PVC로 바인딩 → MySQL에 마운트

### 2-1) PV/PVC/Service/Deployment 매니페스트 작성

아래를 k8s/mysql.yaml로 저장(예시).

⚠️ 주의: 로컬 K8s 종류에 따라 hostPath로 ~/test/mysql를 바로 쓰는 게 안 되는 경우가 있어.
그럴 때는 “Docker Desktop의 HostPath” 지원 범위 또는 “local-path-provisioner” 방식으로 바꿔야 해.

### 2-2) k8s mysql.yaml 실행
```
./01_run_mysql_portforward.sh
```
Pod가 Running이 되면 OK
PVC가 Bound인지 확인


