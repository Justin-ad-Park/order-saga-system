# 쿠버네티스 설정 및 mysql 설치
```
kubectl create namespace msa
kubectl get ns

mkdir ~/test/mysql
```

### 아래 모든 과정은 .sh로 만들어 둠

## Step 2. K8s에 MySQL 배포 (Local Path PV + PVC + Deployment + Service)

목표: ~/test/mysql을 PV로 연결 → PVC로 바인딩 → MySQL에 마운트

### 2-1) PV/PVC/Service/Deployment 매니페스트 작성

아래를 k8s/mysql.yaml로 저장(예시).

⚠️ 주의: 로컬 K8s 종류에 따라 hostPath로 ~/test/mysql를 바로 쓰는 게 안 되는 경우가 있어.
그럴 때는 “Docker Desktop의 HostPath” 지원 범위 또는 “local-path-provisioner” 방식으로 바꿔야 해.

### 2-2) k8s mysql.yaml 실행
```
kubectl apply -f k8s/mysql.yaml
kubectl get pods -w
kubectl get svc -n msa
kubectl get pvc,pv -n msa

```
Pod가 Running이 되면 OK
PVC가 Bound인지 확인

### 2-3) 삭제 방법
```
-- 한방에 삭제 
kubectl delete -f k8s/mysql.yaml 

또는 

kubectl delete deployment mysql -n default
kubectl delete service mysql -n default
kubectl delete pvc mysql-pvc -n default
```

## 3. 쿠버네티스의 mysql에 접속하기 위한 포트 포워드 실행 
```
kubectl port-forward -n msa svc/mysql 3307:3306
```

### 3-1) 스키마(데이터베이스)와 계정 생성은 어디서?

```java
-- 컬럼 순서 강제 조정
ALTER TABLE outbox_message
MODIFY COLUMN coupon_status VARCHAR(255) NOT NULL AFTER order_status;

ALTER TABLE outbox_message
MODIFY COLUMN point_status VARCHAR(255) NOT NULL AFTER coupon_status;

ALTER TABLE outbox_message
MODIFY COLUMN order_status VARCHAR(255) NOT NULL AFTER payment_status ;
```

```mysql
-- Order Orchestrator
CREATE DATABASE IF NOT EXISTS order_orchestrator_db;
CREATE USER IF NOT EXISTS 'order_orchestrator_user'@'%' IDENTIFIED BY 'order_orchestrator_pw';
GRANT ALL PRIVILEGES ON order_orchestrator_db.* TO 'order_orchestrator_user'@'%';

-- Coupon Service
CREATE DATABASE IF NOT EXISTS coupon_db;
CREATE USER IF NOT EXISTS 'coupon_user'@'%' IDENTIFIED BY 'coupon_pw';
GRANT ALL PRIVILEGES ON coupon_db.* TO 'coupon_user'@'%';

-- Point Service
CREATE DATABASE IF NOT EXISTS point_db;
CREATE USER IF NOT EXISTS 'point_user'@'%' IDENTIFIED BY 'point_pw';
GRANT ALL PRIVILEGES ON point_db.* TO 'point_user'@'%';


FLUSH PRIVILEGES;
```

