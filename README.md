# vibe-with-codex

`vibe-with-codex`는 백엔드/아키텍처 실습을 장기적으로 누적하는 Kotlin + Spring Boot 학습 프로젝트입니다.

## 기술 스택

- Kotlin 1.9
- Spring Boot 3.5
- Java 17
- Kotest + JUnit5
- Caffeine

## 학습 영역

- RequestContext 전파/누락 동작 확인
- Cache tier(Local + Redis Mock) 동작 확인
- DDD(애그리거트, 계층 책임, 트랜잭션 경계, 최종 일관성) 모델링 실습

## 학습 문서 인덱스

### DDD

- [2026-04-12_ddd-layered-architecture-dip-order-practice.md](study/2026-04-12_ddd-layered-architecture-dip-order-practice.md)
- [2026-04-12_eventual-consistency-review-summary-practice.md](study/2026-04-12_eventual-consistency-review-summary-practice.md)

### Cache

- [2026-03-22_caffeine-local-cache-deep-dive.md](study/2026-03-22_caffeine-local-cache-deep-dive.md)
- [2026-03-22_caffeine-local-cache-internals-deep-dive.md](study/2026-03-22_caffeine-local-cache-internals-deep-dive.md)
- [2026-03-24_k8s-local-cache-patterns.md](study/2026-03-24_k8s-local-cache-patterns.md)

### RequestContext

- [2026-03-24_request-context-holder-practice.md](study/2026-03-24_request-context-holder-practice.md)

## 실행

```bash
./gradlew test
./gradlew bootRun
```

## 테스트 가이드

```bash
# DDD 주문 모듈
./gradlew test --tests "*OrderDomainTest" --tests "*StudyOrderServiceTest" --tests "*CommerceDomainModelTest" --tests "*AggregateBoundaryServiceTest" --tests "*EventualConsistencyReviewSummaryTest"

# RequestContext 모듈
./gradlew test --tests "*StudyRequestContextServiceTest"

# Cache tier 모듈
./gradlew test --tests "*StudyCacheTierServiceTest" --tests "*StudyCacheTierPropertiesValidationTest"
```
