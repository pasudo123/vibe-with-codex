# Caffeine Local Cache 딥다이브

Spring Boot에서 Caffeine을 사용할 때 핵심 질문은 하나다.

> `@Cacheable` + `CaffeineCacheManager`로 끝낼 것인가, 아니면 native Caffeine API로 계층 캐시 흐름까지 직접 제어할 것인가?

이 문서는 다음 4축으로 정리한다.
- 사용방법
- 동작방식
- 응용방식
- 유의사항(사이드 이펙트)

그리고 내부 구현은 별도 심화 문서로 분리했다.
- 심화: `study/2026-03-22_caffeine-local-cache-internals-deep-dive.md`

---

## TL;DR
- Caffeine은 단순 Map이 아니라 **축출 정책 + 동시성 최적화 + 높은 hit ratio**를 목표로 설계된 캐시다.
- Spring 래핑 방식도 충분히 강력하다. 다만 응답에 source/TTL을 노출하거나 L1/L2 흐름을 세밀하게 제어하려면 native 방식이 더 명확할 수 있다.
- 캐시 품질은 "라이브러리 선택"보다 "TTL/키/무효화/관측성 설계"가 좌우한다.

---

## 1) 사용방법

### 1-1. 의존성

```kotlin
implementation("org.springframework.boot:spring-boot-starter-cache")
implementation("com.github.ben-manes.caffeine:caffeine")
```

### 1-2. 최소 설정 (Spring Bean)

```kotlin
@Bean
fun localCache(
    @Value("\${study.cache.local-ttl-seconds:10}") ttlSeconds: Long,
): Cache<String, Product> {
    return Caffeine.newBuilder()
        // write 이후 ttlSeconds가 지나면 만료
        .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
        // 최대 엔트리 수 제한
        .maximumSize(10_000)
        // 통계 수집 활성화 (Micrometer 연결 시 유용)
        .recordStats()
        .build()
}
```

### 1-3. LoadingCache / AsyncLoadingCache 예시

```kotlin
// miss 시 loader를 통해 자동 적재
val loadingCache = Caffeine.newBuilder()
    .maximumSize(10_000)
    .expireAfterWrite(Duration.ofMinutes(5))
    .build<String, Product> { key ->
        // loader 내부는 느린 IO(DB/API) 가능
        productRepository.findById(key)
            ?: error("not found: $key")
    }

// 비동기 로딩 버전 (CompletableFuture)
val asyncCache = Caffeine.newBuilder()
    .maximumSize(10_000)
    .buildAsync<String, Product> { key, _ ->
        CompletableFuture.supplyAsync {
            productRepository.findById(key)
                ?: error("not found: $key")
        }
    }
```

### 1-4. API 선택 기준 (표)

| API | 추천 상황 | 장점 | 유의사항 |
| --- | --- | --- | --- |
| `Cache<K, V>` | cache-aside를 서비스 코드에서 직접 제어할 때 | 제어점이 가장 많음 | miss 로딩/중복 요청 방지 로직을 직접 설계해야 함 |
| `LoadingCache<K, V>` | miss 로딩 규칙이 명확할 때 | 코드 간결, 자동 로딩 | loader 실패/지연 시 영향 범위를 설계해야 함 |
| `AsyncLoadingCache<K, V>` | 로딩 지연이 크고 비동기 처리가 필요할 때 | 스레드 점유 감소, 병렬 처리 유리 | Future 체인/타임아웃/예외 처리 설계 필요 |

### 1-5. Spring 래핑 사용 예시

```kotlin
@Bean
fun cacheManager(): CacheManager {
    val manager = CaffeineCacheManager("products")
    manager.setCaffeine(
        Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofSeconds(10))
    )
    return manager
}

@Cacheable(cacheNames = ["products"], key = "#id")
fun findProduct(id: String): Product {
    return productRepository.findById(id)
        ?: error("not found")
}
```

출처:
- Caffeine Wiki: https://github.com/ben-manes/caffeine/wiki
- Spring Caffeine 설정: https://docs.spring.io/spring-framework/reference/integration/cache/store-configuration.html#cache-store-configuration-caffeine

---

## 2) 동작방식

### 2-1. Eviction 정책 3축 (표)

| 축 | 주요 옵션 | 언제 유용한가 | 실무 포인트 |
| --- | --- | --- | --- |
| Size | `maximumSize`, `maximumWeight` + `weigher` | 메모리 사용량 상한이 중요할 때 | `maximumWeight`는 객체 "개수"가 아니라 "가중치 합" 제어 |
| Time | `expireAfterWrite`, `expireAfterAccess`, `expireAfter(Expiry)` | 데이터 신선도 제어가 중요할 때 | write 기준/접근 기준의 의미를 API 계약에 명시 |
| Reference | `weakKeys`, `weakValues`, `softValues` | GC 기반 해제 전략이 필요할 때 | 동등성/GC 영향이 커서 기본값(strong)부터 시작 권장 |

### 2-2. Size 기반 상세
- `maximumSize`: 엔트리 수 기반 상한
- `maximumWeight + weigher`: 엔트리별 비용을 정의해 가중치 합으로 제어
- Caffeine은 admission/eviction 정책을 통해 hit ratio를 최대화하려고 시도한다(W-TinyLFU 계열)

### 2-3. Time 기반 상세
- `expireAfterWrite`: 마지막 쓰기 시점 기준 만료
- `expireAfterAccess`: 마지막 접근 시점 기준 만료
- `expireAfter(Expiry)`: 엔트리별 커스텀 만료 정책

### 2-4. Reference 기반 상세
- `weakKeys`: key를 약한 참조로 관리
- `weakValues`, `softValues`: value를 GC 정책과 연계
- 참조 기반 정책은 성능보다 메모리 압력 대응 목적일 때 선택하는 편이 안전하다.

출처:
- Eviction: https://github.com/ben-manes/caffeine/wiki/Eviction
- Design: https://github.com/ben-manes/caffeine/wiki/Design

---

## 3) expire vs refresh (표 + 예시)

### 3-1. 개념 비교

| 항목 | `expireAfterWrite` | `refreshAfterWrite` |
| --- | --- | --- |
| 트리거 | 만료 시점 도달 | 읽기 시점에 refresh 대상 확인 |
| 사용자 체감 | 만료 후 재로딩 전 값 부재 가능 | refresh 중 기존 값 반환 가능 |
| 목적 | 엄격한 유효기간 | 부드러운 갱신(백그라운드 성격) |

### 3-2. 코드 예시

```kotlin
// expire: ttl 이후에는 로딩이 완료되기 전까지 miss가 발생할 수 있음
val expireCache = Caffeine.newBuilder()
    .expireAfterWrite(Duration.ofSeconds(10))
    .build<String, Product> { key -> loadFromStore(key) }

// refresh: 조회 시 refresh가 트리거되고, refresh 중 기존값을 반환할 수 있음
val refreshCache = Caffeine.newBuilder()
    .refreshAfterWrite(Duration.ofSeconds(10))
    .build<String, Product> { key -> loadFromStore(key) }
```

출처:
- Refresh: https://github.com/ben-manes/caffeine/wiki/Refresh

---

## 4) Spring 래핑 vs Native Caffeine 직접제어

### 4-1. 비교 표

| 관점 | Spring 래핑 (`@Cacheable`, `CaffeineCacheManager`) | Native Caffeine + 수동 cache-aside |
| --- | --- | --- |
| 생산성 | 높음 (선언형) | 중간 (직접 구현) |
| 로직 가시성 | 메서드 단위로 분산될 수 있음 | 서비스 플로우에 집중되어 명확 |
| source/TTL 응답 노출 | 기본 추상화 범위 밖 | 직접 구현 가능 |
| L1(Local) + L2(Redis) 세밀 제어 | 추가 설계 필요 | 자연스럽게 구현 가능 |
| 학습 목적 적합성 | 개념 입문에 유리 | 내부 흐름 학습에 유리 |

### 4-2. 왜 이 study는 Native를 선택했는가
현재 study 요구사항은 아래였다.
- 조회 source(`LOCAL/REDIS/MISS`)를 응답으로 내려야 함
- `ttlRemainingSeconds`를 source 기준으로 내려야 함
- `seed` 재적재 시 local invalidate 정책 제어 필요
- local clear API 같은 운영/학습 훅 필요

이 요구사항은 "캐시 적용"보다 "캐시 흐름 자체"가 핵심이라, 수동 cache-aside가 더 직접적이다.

### 4-3. Spring 래핑이 부족하다는 뜻인가?
아니다. Spring 래핑은 메서드 결과 캐싱에는 매우 효율적이다.
다만 이번 주제는 "결과 캐싱"이 아니라 "계층 캐시 오케스트레이션"이 중심이라 선택이 달라진 케이스다.

출처:
- Spring cache store config: https://docs.spring.io/spring-framework/reference/integration/cache/store-configuration.html#cache-store-configuration-caffeine
- Spring `CaffeineCacheManager` Javadoc: https://docs.enterprise.spring.io/spring-framework/docs/6.1.22/javadoc-api/org/springframework/cache/caffeine/CaffeineCacheManager.html

---

## 5) 응용방식

### 5-1. 패턴별 적용 가이드 (표)

| 패턴 | 적합한 상황 | 핵심 구현 포인트 |
| --- | --- | --- |
| Cache-aside | 가장 일반적인 읽기 최적화 | miss 시 하위 저장소 조회 + 상위 캐시 적재 |
| Read-through (LoadingCache) | 로딩 규칙이 일관적일 때 | loader 예외/타임아웃 설계 |
| Write-through/Write-behind | 쓰기 일관성과 처리량 균형 | 실패 재시도/순서 보장 정책 필요 |
| L1 + L2 계층 캐시 | 저지연 + 인스턴스 간 데이터 공유 | TTL 관계, 무효화 전파 전략 설계 |

### 5-2. 운영 관측성 추천
- `recordStats()`로 hit/miss/eviction 지표 수집
- removal listener로 eviction cause 추적
- 지표 예시: hit ratio, load latency, eviction count, stale read 비율

---

## 6) 유의사항 (사이드 이펙트)

### 6-1. 사이드 이펙트 요약표

| 이슈 | 증상 | 원인 | 완화 전략 |
| --- | --- | --- | --- |
| TTL 의미 혼동 | Redis TTL과 응답 TTL이 다르게 보임 | L1/L2 TTL 기준이 다름 | `source`와 TTL 의미를 API 계약으로 고정 |
| stale 데이터 노출 | 최신 쓰기 직후 이전 값 응답 | refresh/비동기 갱신 특성 | 쓰기 시 invalidate, 중요 경로는 expire 기반 설계 |
| stampede | miss 폭주 시 DB/Redis 부하 급증 | 동시 miss 요청 집중 | loading/async, jitter TTL, key 단위 lock |
| reference 정책 부작용 | 예측 어려운 eviction | GC 영향 + 참조 전략 | strong 기본, 메모리 압력 구간에 한정 적용 |
| 테스트 불안정 | 간헐적 실패 | sleep 의존 시간 레이스 | `Ticker` 기반 테스트 고려 |

### 6-2. 체크리스트
- 캐시 목적(성능/일관성/비용절감)을 명시했는가?
- TTL, invalidation, fallback 정책을 문서화했는가?
- 관측 지표 없이 감으로 조정하고 있지 않은가?

---

## 7) 더 깊은 내부 구조 보기

아래 심화 문서에서 Caffeine의 내부 자료구조/버퍼/maintenance 흐름을 더 상세히 다룬다.
- `study/2026-03-22_caffeine-local-cache-internals-deep-dive.md`

---

## 공식 참고자료
- Caffeine GitHub: https://github.com/ben-manes/caffeine
- Caffeine Wiki Home: https://github.com/ben-manes/caffeine/wiki
- Caffeine Design: https://github.com/ben-manes/caffeine/wiki/Design
- Caffeine Eviction: https://github.com/ben-manes/caffeine/wiki/Eviction
- Caffeine Refresh: https://github.com/ben-manes/caffeine/wiki/Refresh
- Spring Caffeine Store Config: https://docs.spring.io/spring-framework/reference/integration/cache/store-configuration.html#cache-store-configuration-caffeine
- Spring `CaffeineCacheManager` Javadoc: https://docs.enterprise.spring.io/spring-framework/docs/6.1.22/javadoc-api/org/springframework/cache/caffeine/CaffeineCacheManager.html
