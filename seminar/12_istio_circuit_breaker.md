# 12. Istio 회로 차단과 강제 지연 테스트

## 목표
- Istio 기반 회로 차단과 timeout 테스트를 재현한다.

## 스토리라인
- 장애가 연속되면 회로 차단이 자동으로 열리고, 회복되는지를 검증.

## 관련 커밋
- `327490d`, `c4401c7`, `8e49e95`, `6161467`, `4b031ed`, `987a667`

## 커밋 변경 요약
| 커밋 | 주요 변경 요약 | 체크아웃 |
| --- | --- | --- |
| `327490d` | istio 설치 및 실행 | `git checkout 327490d` |
| `c4401c7` | Istio 설치. 강제 타임아웃 테스트용 로직 추가, *** 보상 트랜잭션 타이밍 오류 존재함 *** | `git checkout c4401c7` |
| `8e49e95` | SH 정리, Codex에 istio, circuit-breaker 관련 프롬프트 정리 | `git checkout 8e49e95` |
| `6161467` | Istio 설치 경로와 yaml 설정 파일 분리 및 istio 설치 경로 git 제외 | `git checkout 6161467` |
| `4b031ed` | *** Timeout Test용 강제 지연 로직을 SRP, OCP 등을 적용해 Decorator 패턴으로 분리. test, dev Profile에서만 사용하도록 변경 | `git checkout 4b031ed` |
| `987a667` | 04_test_circuit_breaker.sh 정리 - 미사용 변수(쿠폰,포인트), max_time 제거, 불필요한 분기 정리, for 반복 실패 횟수 간략화 | `git checkout 987a667` |

## 핵심 개념
- DestinationRule/VirtualService 설정
- 강제 지연(Decorator 패턴)로 타임아웃 유도

## 기술/기능/프로세스
- 기술: Istio/Envoy, DestinationRule/VirtualService
- 기능: timeout, circuit breaker, 강제 지연 테스트
- MSA: 장애 격리와 회복 검증
- EDA: 이벤트 처리 중 장애 전파 방지
## 데모/실습
- `bin_istio_test/04_test_circuit_breaker.sh`
- 지연 조건: `coupon-service/.../ReserveCouponDelayDecorator.java`, `point-service/.../ReservePointDelayDecorator.java`

## 커밋 상세
### 327490d istio 설치 및 실행
- 주요 변경: istio 설치 및 실행
- 핵심 코드: `bin_istio_test/04_test_circuit_breaker.sh`
```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ORDER_URL="http://localhost:8099/api/v1/orders"
COUPON_BOTH="CPN-INT-BOTH-001"
//--- 생략 ...
```

- 설명: 오케스트레이터가 쿠폰/포인트 예약을 병렬 호출해 분산 트랜잭션의 시작점을 만든다.

### 8e49e95 SH 정리, Codex에 istio, circuit-breaker 관련 프롬프트 정리
- 주요 변경: SH 정리, Codex에 istio, circuit-breaker 관련 프롬프트 정리
- 핵심 코드: `bin_istio_test/04_test_circuit_breaker.sh`
```bash
//--- 생략 ...
  fi
}

echo "==> [1/7] 테스트 데이터 초기화"
"${ROOT_DIR}/bin_common/05_reset_test_data.sh"

echo "==> [2/7] Istio circuit-breaker 적용"
kubectl -n msa apply -f "${ROOT_DIR}/bin_k8s/istio/circuit-breaker.yaml"

//--- 생략 ...
```
- 설명: 실행/테스트 스크립트를 통해 분산 시나리오를 재현한다.

