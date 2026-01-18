# 공통 데모/실습 명령어

## 로컬 테스트 준비
- `bin_test/01_prepare_local_order_saga_test.sh`

## K8s(Dev) 준비
- `bin_common/02_prepare_k8s_order_saga_Local_Consumer.sh`
- `bin_common/03_prepare_k8s_order_saga_all_k8s.sh`

## 테스트 데이터 초기화/스냅샷
- 스냅샷 생성: `bin_common/04_create_test_snapshot_procs.sh`
- 리셋: `bin_common/05_reset_test_data.sh`

## Kafka 토픽 확인/정리 (테스트)
- 관련 테스트 코드: `order-orchestrator/src/test/java/com/example/orderorchestrator/adapter/out/kafka/*`

## Istio 회로 차단기 테스트
- `bin_istio_test/04_test_circuit_breaker.sh`
- `bin_istio_test/05_test_saga_compensation.sh`
