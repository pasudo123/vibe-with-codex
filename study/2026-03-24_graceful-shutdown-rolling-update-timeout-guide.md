# Graceful Shutdown과 롤링 배포 타임아웃 실전 가이드

A 서비스가 B 서비스를 호출하는 구조에서, B 배포 시 A 타임아웃이 발생하는 전형적인 원인과 대응을 학습용 코드로 정리했다.

## 요약
- 배포 중 타임아웃은 보통 3가지 축에서 발생한다.
  - **웜업 미완료**: 신규 Pod가 충분히 준비되기 전에 트래픽을 받음
  - **종료 중 Pod 유입**: 내려가는 Pod(draining/terminating)으로 호출이 유입됨
  - **호출 정책 불일치**: caller timeout/retry/connection 재사용 전략이 rollout 구간과 맞지 않음
- Spring Boot는 graceful shutdown을 지원하며, 종료 시 기존 요청 완료 시간을 `spring.lifecycle.timeout-per-shutdown-phase`로 제어할 수 있다.
- Kubernetes는 readiness/liveness/startup probe와 rolling update 전략(`maxUnavailable`, `maxSurge`)으로 배포 중 가용성을 조절한다.

## 사용법
### 1) 앱 실행
```bash
./gradlew bootRun
```

### 2) 시나리오 호출
```bash
http POST :8080/study/graceful-shutdown/scenarios/READY_BASELINE/run timeoutMs==300 retries==0
http POST :8080/study/graceful-shutdown/scenarios/WARMUP_MISS/run timeoutMs==100 retries==0
http POST :8080/study/graceful-shutdown/scenarios/DRAINING_POD_HIT/run timeoutMs==100 retries==0
http POST :8080/study/graceful-shutdown/scenarios/RETRY_RECOVERS/run timeoutMs==120 retries==1
```

### 3) 상태 조회
```bash
http GET :8080/study/graceful-shutdown/state
```

### 4) Kubernetes 예시 매니페스트 확인
```bash
kubectl apply -f k8s/graceful-shutdown/a-b-rollout-example.yaml
```

## 동작 방식
### 1) 시뮬레이션 모델
- `BPodState`를 `WARMING_UP`, `READY`, `DRAINING`, `TERMINATED`로 나눠 B의 배포 순간 상태를 재현한다.
- A는 timeout/retry를 적용해 B를 호출하고, 각 시도 결과를 `attempts`로 기록한다.

### 2) 왜 타임아웃이 나는가
- `WARMUP_MISS`: B가 준비되기 전 요청이 들어오면 응답 지연으로 A timeout이 발생한다.
- `DRAINING_POD_HIT`: B가 종료 중인 시점에 요청을 받으면 연결 종료/지연으로 timeout 또는 실패가 발생할 수 있다.
- `RETRY_RECOVERS`: 첫 시도는 draining에서 실패하더라도, 다음 시도에 READY Pod로 라우팅되면 회복된다.

### 3) 운영에서 확인할 포인트
- Pod 종료 시점에 readiness가 빠르게 내려가는지
- `preStop` + `terminationGracePeriodSeconds`가 실제 요청 종료 시간보다 충분한지
- rollout 중 `maxUnavailable`가 너무 커서 가용 Pod 수가 급감하지 않는지
- A의 timeout/retry/backoff가 B의 배포 전환 시간과 맞는지
- `k8s/graceful-shutdown/a-b-rollout-example.yaml` 기준으로
  - B: `maxUnavailable: 0`, `maxSurge: 1`, `preStop`, `terminationGracePeriodSeconds`
  - B: `startupProbe + readinessProbe + livenessProbe`
  - A: 짧은 timeout + 1회 retry 기본값

## 응용
- 실제 서비스에서는 아래 조합이 일반적으로 안전하다.
  - B: readiness probe + startup probe + graceful shutdown 활성화
  - B: `preStop`에서 드레이닝 시작, readiness 실패 전환
  - A: 짧은 timeout + 제한된 retry(지수 backoff 포함)
- 필요 시 `EndpointSlice`의 `terminating/serving/ready` 관점으로 트래픽 흐름을 점검한다.

## 유의사항
- readiness가 true인 동안에는 종료 중 Pod로 트래픽이 남아 있을 수 있다. 배포 도구/프록시/서비스메시 특성까지 함께 봐야 한다.
- timeout을 무작정 늘리면 장애를 늦게 감지할 수 있다. retry와 함께 균형을 맞춘다.
- 별도 management port에서 probe를 볼 때는 실제 트래픽 수용 경로와 분리되어 오탐이 생길 수 있으니 구성 의도를 명확히 한다.

## 참고 코드
- API
  - `POST /study/graceful-shutdown/scenarios/{scenarioKey}/run`
  - `GET /study/graceful-shutdown/state`
- 시나리오 키
  - `READY_BASELINE`, `WARMUP_MISS`, `DRAINING_POD_HIT`, `RETRY_RECOVERS`

## 공식 문서
- Spring Boot Graceful Shutdown: https://docs.spring.io/spring-boot/reference/web/graceful-shutdown.html
- Spring Boot Kubernetes Probes (Actuator): https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.kubernetes-probes
- Kubernetes Pod Lifecycle / Termination: https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#pod-termination
- Kubernetes Probes: https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/
- Kubernetes EndpointSlice Conditions: https://kubernetes.io/docs/concepts/services-networking/endpoint-slices/#conditions
- Kubernetes Deployment Rolling Update: https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#rolling-update-deployment
