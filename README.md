# Kotest Style 샘플

이 프로젝트는 Kotest의 여러 스타일을 `Slugifier` 예제로 학습할 수 있도록 구성되어 있습니다.

## 실행 방법

```bash
./gradlew test
```

## 테스트 스타일별 예제 파일

- FunSpec: `src/test/kotlin/com/vibewithcodex/core/SlugifierTest.kt`
- StringSpec: `src/test/kotlin/com/vibewithcodex/core/SlugifierStringSpecTest.kt`
- ShouldSpec: `src/test/kotlin/com/vibewithcodex/core/SlugifierShouldSpecTest.kt`
- DescribeSpec: `src/test/kotlin/com/vibewithcodex/core/SlugifierDescribeSpecTest.kt`
- BehaviorSpec: `src/test/kotlin/com/vibewithcodex/core/SlugifierBehaviorSpecTest.kt`
- ExpectSpec: `src/test/kotlin/com/vibewithcodex/core/SlugifierExpectSpecTest.kt`
- WordSpec: `src/test/kotlin/com/vibewithcodex/core/SlugifierWordSpecTest.kt`
- FreeSpec: `src/test/kotlin/com/vibewithcodex/core/SlugifierFreeSpecTest.kt`

## Style 선택 가이드

Kotest의 스타일은 기능상 동일하며, 팀이 "읽기 쉬운 문장 구조"를 합의하는 것이 핵심입니다.

| Style | 언제 쓰면 좋은가 | 장점 | 주의점 |
| --- | --- | --- | --- |
| FunSpec | 범용 기본형이 필요할 때, 테스트 이름을 명확히 적고 싶을 때 | 구조가 단순해 학습 비용이 낮음 | 테스트 구조가 커지면 구획(섹션) 설계가 필요 |
| StringSpec | 테스트 이름만 빠르게 쓰고 싶을 때 | 가장 간결함 | 중첩 구조가 약해 큰 시나리오엔 부적합 |
| ShouldSpec | “~해야 한다” 식의 문장형 테스트를 선호할 때 | 자연어에 가까워 의도가 잘 보임 | 지나친 중첩은 가독성을 해칠 수 있음 |
| DescribeSpec | `describe/it` 구조로 BDD 흐름을 만들고 싶을 때 | 읽는 사람에게 친숙한 BDD 구조 | 문장이 길어지면 테스트 이름이 모호해질 수 있음 |
| BehaviorSpec | `given/when/then` 흐름을 강조하고 싶을 때 | 시나리오 흐름이 명확 | 단일 행위 테스트에는 다소 장황 |
| ExpectSpec | “기대 결과”를 중심으로 테스트를 읽히게 하고 싶을 때 | 기대값 중심의 서술이 명확 | 조건 설명이 부족하면 맥락이 희미해짐 |
| WordSpec | 자연어에 가까운 `should` 문장을 중첩해 쓰고 싶을 때 | 문장 형태로 읽기 쉬움 | 중첩이 깊어지면 스코프 파악이 어려움 |
| FreeSpec | 자유로운 계층 구조로 시나리오를 나누고 싶을 때 | 복잡한 도메인 분류에 유리 | 구조가 자유로운 만큼 규칙 합의가 필요 |

### 빠른 선택 팁

- 팀 표준이 없다면: FunSpec
- BDD 문화가 강하면: DescribeSpec 또는 BehaviorSpec
- 시나리오 분류가 복잡하면: FreeSpec
- 짧고 단순한 테스트가 대부분이면: StringSpec

## 공식 문서 링크

- Kotest Framework: `https://kotest.io/docs/framework/framework.html`
- Testing Styles: `https://kotest.io/docs/5.9.x/framework/testing-styles.html`
