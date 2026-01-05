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
./bin_k8s/01_apply_mysql.sh

```
Pod가 Running이 되면 OK
PVC가 Bound인지 확인

### MySQL 중지
```
./bin_k8s/_01_stop_mysql.sh
```

### MySQL 삭제 방법
```
# 한방에 삭제 
kubectl delete -f k8s/mysql.yaml 
# 또는
./bin_k8s/_03_delete_all_deploy_msa.sh
```

## 3. 쿠버네티스의 mysql에 접속하기 위한 포트 포워드 실행 
```
./bin_k8s/02_portforward.sh
```

### 3-1) mySql에 접속해서 스키마(데이터베이스)와 계정 생성
URL: jdbc:mysql://localhost:3307
user: root
pwd: rootpw


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


```java
/* 컬럼 순서 강제 조정 */
ALTER TABLE outbox_message
MODIFY COLUMN saga_status VARCHAR(255) NOT NULL AFTER payload;

ALTER TABLE outbox_message
MODIFY COLUMN order_status VARCHAR(255) NOT NULL AFTER payload;

ALTER TABLE outbox_message
MODIFY COLUMN point_status VARCHAR(255) NOT NULL AFTER payload;

ALTER TABLE outbox_message
MODIFY COLUMN coupon_status VARCHAR(255) NOT NULL AFTER payload;

```

## MSA K8s에 배포 및 실행
```
./bin_k8s/03_deploy_all.sh


## 종료 
./bin_k8s/_03_stop_msa.sh

## 삭제 
./bin_k8s/_03_delete_all_deploy_msa.sh

```

### 수정 사항 재배포 
```
./bin_k8s/04_restart_msa.sh
```

## 카프카 배포 및 로컬 포트포워드
```
./bin_k8s/06_deploy_kafka.sh

## 카프카 포트포워드 종료 및 리소스 삭제
./bin_k8s/_06_kill_kafka.sh
```



