# A/B 롤링 배포 예시

`a-b-rollout-example.yaml`은 A -> B 호출 구조에서 배포 중 타임아웃을 줄이기 위한 최소 설정 예시다.

## 포함 리소스
- `Service` x2 (`a-service`, `b-service`)
- `Deployment` x2 (`a-service`, `b-service`)
- `PodDisruptionBudget` x1 (`b-service-pdb`)

## 핵심 포인트
- B Deployment
  - `maxUnavailable: 0`, `maxSurge: 1`
  - `terminationGracePeriodSeconds: 40`
  - `preStop: sleep 10`
  - `startupProbe`, `readinessProbe`, `livenessProbe`
- A Deployment
  - B 호출 timeout/retry 환경변수 예시 포함

## 적용
```bash
kubectl apply -f k8s/graceful-shutdown/a-b-rollout-example.yaml
```
