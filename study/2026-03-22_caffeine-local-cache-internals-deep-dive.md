# Caffeine 내부구조 심화: Low-level 동작 관점

이 문서는 `study/2026-03-22_caffeine-local-cache-deep-dive.md`의 심화편이다.
목표는 "API 사용법"이 아니라 "왜 이런 성능/정책 특성이 나오는가"를 내부 구조 관점에서 이해하는 것이다.

---

## 1) 큰 그림: Caffeine이 빠른 이유

Caffeine은 단순히 `ConcurrentHashMap + LRU`가 아니다.
설계 문서 기준으로 핵심은 아래 조합이다.

1. **Admission + Eviction 분리**
   - 새 엔트리를 무조건 넣지 않고, 들어올 가치가 있는지(빈도/최근성) 평가
2. **W-TinyLFU 계열 정책**
   - 최근성(window) + 빈도(sketch) 정보를 같이 사용해 hit ratio를 개선
3. **읽기/쓰기 버퍼링**
   - 접근 이벤트를 바로 전역 구조에 반영하지 않고 배치 반영
4. **락 경쟁 완화**
   - 고경합 상황에서도 전체 구조를 매번 잠그지 않도록 설계

출처:
- Design: https://github.com/ben-manes/caffeine/wiki/Design

---

## 2) 자료구조 관점

### 2-1. 왜 단일 LRU가 아닌가
단일 LRU는 burst 패턴에서 hit ratio가 급락하기 쉽다.
Caffeine은 window/probation/protected 계층(또는 유사 분할 개념)을 통해 다음을 노린다.
- 신규 유입 트래픽 흡수(window)
- 일회성 접근과 반복 접근 분리(probation/protected)
- 자주 쓰는 항목 보호

### 2-2. 주기적 maintenance가 필요한 이유
size/time/reference 정책은 "순간 계산"이 아니라 "정책 반영 작업"이 필요하다.
그래서 Caffeine은 읽기/쓰기 이벤트를 큐/버퍼에 모은 뒤 maintenance에서 정리한다.
- 장점: hot path 비용 완화
- 트레이드오프: 만료/정리가 완전히 즉시 반영되지 않을 수 있음

### 2-3. frequency sketch 역할
TinyLFU 계열 아이디어는 최근 접근 빈도를 압축 구조로 추적해,
"기존 항목 vs 신규 항목" 중 누가 더 캐시에 남을 가치가 높은지 판단하는 데 사용된다.

---

## 3) 읽기/쓰기 경로를 코드 흐름으로 보면

아래는 cache-aside 서비스에서 자주 보이는 흐름을 Caffeine 내부 관점으로 분해한 것이다.

```kotlin
fun getData(key: String): Response {
    // 1) local cache read
    cache.getIfPresent(key)?.let { return hit(it) }

    // 2) lower tier read (redis/db)
    val loaded = loadFromRedis(key) ?: return miss()

    // 3) local cache write
    cache.put(key, loaded)
    return loadedFromRedis(loaded)
}
```

내부적으로 중요한 지점:
- `getIfPresent`
  - 단순 조회 + 접근 기록 이벤트(정책 반영용) 축적
- `put`
  - write 경로에서 size/time/reference 정책 적용 후보가 됨
- maintenance
  - 버퍼된 접근/쓰기 이벤트를 소모하며 eviction/expiration 수행

즉, API는 단순하지만 내부는 "정책 엔진"이 계속 동작하는 구조다.

---

## 4) Size/Time/Reference가 동시에 있을 때

### 4-1. 평가 순서에 대한 실무적 해석
정확한 내부 순서를 외워서 쓰기보다, 운영 관점에서는 아래처럼 이해하면 안전하다.
- Size는 메모리 상한을 강제
- Time은 신선도를 강제
- Reference는 GC 압력에 반응

그리고 이 조건들은 독립이 아니라 함께 적용된다.
예: time 만료 대상이 많아도 size 압력이 먼저 관측될 수 있고, GC 영향으로 reference 기반 해제가 먼저 관측될 수도 있다.

### 4-2. 운영에서 보이는 현상
- "아직 TTL 남았는데 왜 miss?"
  - size/references로 먼저 탈락했을 가능성
- "TTL 지났는데 즉시 안 사라짐"
  - maintenance 주기/접근 트리거 영향

이 때문에 지표 없이 체감만으로 정책을 조정하면 실패하기 쉽다.

---

## 5) refreshAfterWrite를 low-level로 해석

`refreshAfterWrite`는 "만료"가 아니라 "갱신 대상 표시"에 가깝다.

동작 해석:
1. entry가 refresh 대상 시간이 됨
2. read가 들어오면 refresh 트리거
3. refresh 중에는 기존 값을 계속 반환 가능
4. refresh 완료 후 새 값 반영

장점:
- 읽기 경로에서 지연 스파이크 완화

주의:
- 강한 최신성 보장이 필요한 구간에는 부적합할 수 있음

출처:
- Refresh: https://github.com/ben-manes/caffeine/wiki/Refresh

---

## 6) Spring 래핑에서 내부를 얼마나 제어할 수 있나

`CaffeineCacheManager`로도 빌더 설정은 충분히 가능하다.
- `setCaffeine`, `setCaffeineSpec`, `setCacheSpecification`
- cache별 custom cache 등록
- async mode 설정

하지만 아래는 여전히 애플리케이션 설계 문제다.
- 응답 payload에 source/TTL을 어떤 기준으로 노출할지
- L1/L2 재적재 시점과 invalidate 정책
- seed/reload/clear 같은 운영 훅

즉, 내부 정책 설정은 Spring에서도 가능하지만,
"흐름/의미" 제어는 보통 서비스 레이어 설계에서 결정된다.

출처:
- Spring cache store config: https://docs.spring.io/spring-framework/reference/integration/cache/store-configuration.html#cache-store-configuration-caffeine
- `CaffeineCacheManager` Javadoc: https://docs.enterprise.spring.io/spring-framework/docs/6.1.22/javadoc-api/org/springframework/cache/caffeine/CaffeineCacheManager.html

---

## 7) 실전 튜닝 순서 (권장)

1. 기본값으로 시작
   - `maximumSize`, `expireAfterWrite`, `recordStats`
2. 지표 확보
   - hit ratio, load latency, eviction count
3. 병목 원인 분리
   - 키 설계 문제인지, TTL 문제인지, size 상한 문제인지
4. 정책 조정
   - 필요 시 `maximumWeight/weigher`, `refreshAfterWrite`, 계층 캐시 전략 추가
5. 회귀 검증
   - load test + 장애 상황 fallback 점검

---

## 8) 핵심 요약

- Caffeine의 강점은 "빠른 Map"이 아니라 "정책 엔진"이다.
- hit ratio와 처리량은 내부 구조(window/frequency/buffer/maintenance)에서 나온다.
- Spring 래핑 vs native 선택은 기능 우열이 아니라 제어 지점 선택이다.
- 운영 성공은 정책보다 측정(지표)과 의미 계약(source/TTL)에서 갈린다.
