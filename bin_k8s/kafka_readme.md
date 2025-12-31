# Kafka 테스트 (K8S 내부 + 로컬 Port-forward)

## 1) 토픽 생성 (K8S 내부)
```
kubectl -n msa exec deploy/kafka -- /bin/bash -lc \
"/opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --create --if-not-exists --topic order-events --partitions 1 --replication-factor 1"
```

## 2) 이벤트 발행 (로컬에서)
```
## kcat이 없으면 brew install kcat로 설치하면 됩니다.

echo "order-created-1" | kcat -b localhost:9094 -t order-events -P
```

## 3) 이벤트 소비 (로컬에서)
```
kcat -b localhost:9094 -t order-events -C -o beginning
```

## 4) 브로커 접속 확인
```
kcat -b localhost:9094 -L
```

## 5) K8S 내부 서비스에서 접속
```
bootstrap.servers=kafka:9092
```
