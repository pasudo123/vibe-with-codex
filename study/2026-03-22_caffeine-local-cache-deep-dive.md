# Caffeine Local Cache 딥다이브

Spring Boot에서 Caffeine을 사용할 때, "그냥 @Cacheable로 끝낼지" 또는 "Caffeine API를 직접 제어할지"는 기능 문제가 아니라 **제어 지점(control point)** 선택 문제다.

이 글은 아래 4가지를 중심으로 정리한다.
- 사용방법
- 동작방식
- 응용방식
- 유의사항(사이드 이펙트)

---

## TL;DR
- Caffeine은 단순 Map이 아니라, 높은 적중률과 처리량을 목표로 설계된 고성능 로컬 캐시다.
- Spring Cache Abstraction(`@Cacheable`, `CaffeineCacheManager`)도 충분히 강력하다.
- 다만 L1(Local) + L2(Redis)처럼 **계층 흐름과 응답 형식(source/TTL)을 직접 통제**해야 하면 native 방식이 더 명확할 때가 있다.
- 실무에서는 "추상화 레벨을 어디까지 올릴지"가 핵심 결정 포인트다.

---

## 1) 사용방법

### 1-1. 의존성

```kotlin
implementation("org.springframework.boot:spring-boot-starter-cache")
implementation("com.github.ben-manes.caffeine:caffeine")
```

### 1-2. 최소 설정 예시 (Spring Bean)

```kotlin
@Bean
fun studyLocalCache(
  @Value("\${study.cachetier.local-ttl-seconds:10}") ttl: Long,
): Cache<String, LocalCacheValue> {
  return Caffeine.newBuilder()
    .expireAfterWrite(Duration.ofSeconds(ttl))
    .maximumSize(10_000)
    .build()
}
```

### 1-3. API 선택 기준
- `Cache<K, V>`: put/get/invalidate를 직접 제어
- `LoadingCache<K, V>`: miss 시 자동 로딩
- `AsyncLoadingCache<K, V>`: 비동기 로딩(CompletableFuture 기반)

출처:
- Caffeine Wiki: https://github.com/ben-manes/caffeine/wiki

---

## 2) 동작방식

### 2-1. 축출(Eviction) 정책
Caffeine은 정책을 조합해 동작한다.
- Size: `maximumSize`, `maximumWeight + weigher`
- Time: `expireAfterAccess`, `expireAfterWrite`, `expireAfter(Expiry)`
- Reference: `weakKeys`, `weakValues`, `softValues`

출처:
- Eviction: https://github.com/ben-manes/caffeine/wiki/Eviction

### 2-2. 내부 설계 핵심
Caffeine은 동시성 환경에서 락 경합을 줄이고 처리량을 올리기 위해 버퍼/큐 기반 구조를 사용한다.
그리고 적중률 관점에서는 W-TinyLFU 계열 정책으로 알려져 있다.

출처:
- Design: https://github.com/ben-manes/caffeine/wiki/Design

### 2-3. expire vs refresh
- `expireAfterWrite`: 만료되면 해당 시점 이후 새 로딩 전까지 값이 비어있을 수 있음
- `refreshAfterWrite`: 조회가 들어올 때 refresh가 트리거되며, refresh 중에는 기존 값이 반환될 수 있음

출처:
- Refresh: https://github.com/ben-manes/caffeine/wiki/Refresh

---

## 3) Spring 래핑 vs Native Caffeine: 왜 직접 제어를 선택했나

여기서 핵심은 "Spring이 Caffeine을 지원하느냐"가 아니다. 지원한다.
문제는 **요구사항을 어디서 표현할지**다.

### 3-1. Spring 방식이 잘 맞는 경우
- 선언형 캐싱(`@Cacheable`, `@CacheEvict`)으로 충분할 때
- 캐시 키/조건/무효화를 메서드 단위로 단순하게 다룰 때
- 캐시 내부 메타데이터(TTL source 구분 등)를 응답에 노출할 필요가 없을 때

Spring 공식 문서와 Javadoc을 보면 Caffeine 연동 옵션이 풍부하다.
- `CaffeineCacheManager` 기본 연동
- `setCaffeine`, `setCaffeineSpec`, `setCacheSpecification`
- `registerCustomCache`로 cache별 개별 설정
- 6.1+에서 async cache mode 지원

출처:
- Spring cache store config: https://docs.spring.io/spring-framework/reference/integration/cache/store-configuration.html#cache-store-configuration-caffeine
- CaffeineCacheManager Javadoc: https://docs.enterprise.spring.io/spring-framework/docs/6.1.22/javadoc-api/org/springframework/cache/caffeine/CaffeineCacheManager.html

### 3-2. Native 방식을 택한 이유 (이 프로젝트 기준)
이 저장소의 학습 요구사항은 아래였다.
- L1(Local) -> L2(Redis mock) 순서 제어
- `source`(LOCAL/REDIS/MISS)를 응답으로 노출
- 조회 시점의 `ttlRemainingSeconds`를 source 기준으로 내려주기
- 재seed 시 local invalidate 같은 세밀한 제어
- 수동 local clear API 제공

이런 요구는 선언형 캐시만으로 표현하면 로직이 흩어지거나, 응답 의미를 맞추기 어렵다.
그래서 서비스 레이어에서 cache-aside를 수동 구현해 흐름을 명확히 보이게 했다.

정리:
- Spring 래핑이 "부족해서"가 아니라,
- 이번 요구사항이 "흐름/메타데이터를 직접 통제"하는 쪽에 가까워 native가 더 적합했다.

---

## 4) 응용방식

### 4-1. L1(Local) + L2(Redis) 패턴
전형적인 읽기 흐름:
1. Local 조회
2. miss면 Redis 조회
3. Redis hit면 Local 재적재

이 구조는 지연시간과 백엔드 부하를 동시에 줄이는 데 유리하다.

### 4-2. 운영 확장 포인트
- `recordStats()` + 메트릭 연동(Micrometer)
- `removalListener`로 축출 원인 관찰
- 캐시별 정책 분리(핫키 vs 콜드키)
- stampede 완화: loading/async loading, jitter TTL, 단건 락

### 4-3. API 설계 팁
2계층 캐시라면 `source`를 응답에 포함하는 것을 권장한다.
- TTL 필드가 하나여도 source와 함께 보면 의미가 분명해진다.

---

## 5) 유의사항 (사이드 이펙트)

### 5-1. TTL 오해
L1/L2의 TTL은 독립적일 수 있다.
- 증상: "Redis TTL 30인데 응답은 10/60으로 보임"
- 원인: 응답 TTL이 Local 기준으로 계산됨
- 완화: `source` + TTL 의미를 API 문서로 고정

### 5-2. refreshAfterWrite 오해
- 증상: refresh를 설정했는데 즉시 최신값이 안 보임
- 원인: refresh는 조회 트리거 + 기존값 반환 가능
- 완화: 강한 일관성이 필요하면 expire/수동 갱신과 함께 설계

### 5-3. weak/soft reference 부작용
- 증상: 예상과 다른 hit/miss 또는 비교 동작
- 원인: reference 기반 정책의 동등성/GC 영향
- 완화: 기본 strong reference부터 시작, 필요 시 제한적으로 도입

### 5-4. weight 정책 착시
- 증상: 객체 크기 변화가 축출에 반영되지 않음
- 원인: weight는 생성/업데이트 시점 계산 후 고정
- 완화: 값 구조 변경 시 갱신 정책/재적재 전략 설계

### 5-5. 시간 의존 테스트의 불안정성
- 증상: 간헐적 실패
- 원인: `Thread.sleep` 기반 타이밍 레이스
- 완화: 가능하면 `Ticker` 기반 테스트로 전환

---

## 6) 실전 체크리스트
- 이 캐시는 성능 최적화용인가, 일관성 보장용인가?
- TTL은 비즈니스 신선도 요구와 맞는가?
- source/TTL 의미가 API 계약으로 고정돼 있는가?
- invalidate 전략(쓰기/이벤트/수동)이 정의돼 있는가?
- hit/miss/eviction 지표를 관찰하고 있는가?

---

## 공식 참고자료
- Caffeine GitHub: https://github.com/ben-manes/caffeine
- Caffeine Wiki Home: https://github.com/ben-manes/caffeine/wiki
- Caffeine Design: https://github.com/ben-manes/caffeine/wiki/Design
- Caffeine Eviction: https://github.com/ben-manes/caffeine/wiki/Eviction
- Caffeine Refresh: https://github.com/ben-manes/caffeine/wiki/Refresh
- Spring Caffeine Store Config: https://docs.spring.io/spring-framework/reference/integration/cache/store-configuration.html#cache-store-configuration-caffeine
- Spring `CaffeineCacheManager` Javadoc: https://docs.enterprise.spring.io/spring-framework/docs/6.1.22/javadoc-api/org/springframework/cache/caffeine/CaffeineCacheManager.html
