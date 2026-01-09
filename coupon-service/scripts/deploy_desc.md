# deploy_k8s.sh 설명 (초보자용)

이 문서는 `deploy_k8s.sh`가 무엇을 하고, 왜 필요한지 초보자도 이해할 수 있도록 풀어서 설명합니다.

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SERVICE_DIR="${ROOT_DIR}/coupon-service"
MANIFEST="${ROOT_DIR}/bin_k8s/coupon-service.yaml"
IMAGE_NAME="coupon-service:local"

"${ROOT_DIR}/gradlew" :coupon-service:bootJar
docker build -t "${IMAGE_NAME}" "${SERVICE_DIR}"
kubectl apply -f "${MANIFEST}"
kubectl set env -n msa deployment/coupon-service SPRING_PROFILES_ACTIVE=dev
```

## 한눈에 보기
이 스크립트는 쿠폰 서비스(coupon-service)를 **빌드 → 도커 이미지 생성 → 쿠버네티스 배포 → 환경변수 설정** 순서로 자동 실행합니다.

## 스크립트 단계별 설명

### 1) 안전장치 설정
```bash
set -euo pipefail
```
- `-e`: 명령이 실패하면 바로 중단합니다.
- `-u`: 선언하지 않은 변수를 쓰면 에러로 중단합니다.
- `-o pipefail`: 파이프(`|`)로 연결된 명령 중 하나라도 실패하면 전체 실패로 처리합니다.

### 2) 경로와 이름 준비

```bash 
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SERVICE_DIR="${ROOT_DIR}/coupon-service"
MANIFEST="${ROOT_DIR}/bin_k8s/coupon-service.yaml"
IMAGE_NAME="coupon-service:local"

- `ROOT_DIR`: 현재 스크립트 위치 기준으로 프로젝트 루트 경로를 계산합니다.
- `SERVICE_DIR`: coupon-service 폴더 경로입니다.
- `MANIFEST`: 쿠버네티스 배포 파일(`coupon-service.yaml`) 경로입니다.
- `IMAGE_NAME`: 만들 도커 이미지 이름입니다(`coupon-service:local`).
```

- [ROOT_DIR_desc.md](ROOT_DIR_desc.md)



### 3) 애플리케이션 빌드 (JAR 생성)
```bash
"${ROOT_DIR}/gradlew" :coupon-service:bootJar
```
- Gradle을 사용해 `coupon-service` 모듈의 실행 JAR 파일을 만듭니다.

### 4) 도커 이미지 만들기
```bash
docker build -t "${IMAGE_NAME}" "${SERVICE_DIR}"
```
- `coupon-service` 폴더의 `Dockerfile`을 이용해 도커 이미지를 생성합니다.
- 이미지 이름은 `coupon-service:local`로 붙습니다.
- 
#### 생성 위치와 확인 방법
- 생성된 이미지는 로컬 Docker 데몬의 이미지 저장소에 저장됩니다(별도 파일로 보이지 않음).
- 확인: `docker images | rg coupon-service` 또는 `docker image ls coupon-service:local`

### 5) 쿠버네티스에 배포
```bash
kubectl apply -f "${MANIFEST}"
```
- 쿠버네티스 설정 파일(`coupon-service.yaml`)을 적용하여
  Deployment, Service 등의 리소스를 생성/업데이트합니다.

### 6) 환경변수 설정 (프로파일: dev)
```bash
kubectl set env -n msa deployment/coupon-service SPRING_PROFILES_ACTIVE=dev
```
- `msa` 네임스페이스의 `coupon-service` Deployment에
  `SPRING_PROFILES_ACTIVE=dev` 환경변수를 설정합니다.
- 스프링 부트가 `dev` 프로파일로 실행됩니다.

## 실행 전 필요한 것
- `docker`와 `kubectl`이 설치되어 있어야 합니다.
- 쿠버네티스 클러스터가 실행 중이어야 합니다.
- `kubectl`이 올바른 클러스터/네임스페이스를 가리키고 있어야 합니다.

## 요약
`deploy_k8s.sh`는 쿠폰 서비스를 빌드하고 도커 이미지로 만든 뒤,
쿠버네티스에 배포하고 `dev` 프로파일을 활성화합니다.
