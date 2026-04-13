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
- 실무형 Cache tier(Local + Redis Mock + DB Mock) 패턴 실습
- DDD(애그리거트, 계층 책임, 트랜잭션 경계, 최종 일관성) 모델링 실습

## 학습 문서 인덱스

### DDD

- [2026-04-12_ddd-layered-architecture-dip-order-practice.md](study/2026-04-12_ddd-layered-architecture-dip-order-practice.md)
- [2026-04-12_eventual-consistency-review-summary-practice.md](study/2026-04-12_eventual-consistency-review-summary-practice.md)

DDD 패키지 구조(바운디드 컨텍스트 기준):

- `src/main/kotlin/com/vibewithcodex/study/ddd/ordering`
- `src/main/kotlin/com/vibewithcodex/study/ddd/catalog`
- `src/main/kotlin/com/vibewithcodex/study/ddd/review`
- `src/main/kotlin/com/vibewithcodex/study/ddd/member`
- `src/main/kotlin/com/vibewithcodex/study/ddd/shipping`
- `src/main/kotlin/com/vibewithcodex/study/ddd/shared`

### Cache

- [2026-04-13_cache-layer-practical-patterns.md](study/2026-04-13_cache-layer-practical-patterns.md)
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
./gradlew test --tests "*StudyCacheTier*"
```
