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

Kotest의 스타일은 기능상 동일하며, 어떤 구조로 읽히게 할지의 선택입니다.

| Style | 공식 설명 기반 특징 | 추천 상황 | 주의점 |
| --- | --- | --- | --- |
| FunSpec | `test("...")`로 정의하는 기본형이며, 확신이 없으면 이 스타일을 사용하라고 안내됨 | 팀 표준이 없거나 가장 무난한 기본형이 필요할 때 | 구조가 커지면 `context`로 구획을 명확히 나누는 규칙이 필요 |
| ShouldSpec | `should("...")`로 테스트를 정의하고 `context`로 중첩 가능 | “~해야 한다” 문장형 테스트를 선호할 때 | 문장이 길어지면 기대사항이 흐려질 수 있음 |
| DescribeSpec | `describe/it` (또는 `context`)로 중첩되며 Ruby/JS 배경에 익숙한 스타일 | BDD 서술형 구조를 선호하거나 JS/RSpec 경험이 있는 팀 | 중첩이 깊어지면 테스트 이름이 길어질 수 있음 |
| BehaviorSpec | `context/given/when/then` 흐름의 BDD 스타일 | 시나리오 흐름을 단계적으로 보여주고 싶을 때 | Kotlin 키워드 `when`은 백틱이 필요하며, `Then`에는 `And`가 없다는 제한이 있음 |
| WordSpec | `context string` + `should`로 중첩하며 `When` 중첩 지원 | 자연어 문장 형태의 중첩 테스트를 선호할 때 | 중첩 깊이가 커지면 스코프 파악이 어려울 수 있음 |
| FreeSpec | `-`로 자유로운 계층 중첩을 허용 | 복잡한 도메인 분류/계층 구조가 필요할 때 | 최하위 테스트에서 `-`를 쓰면 안 된다는 규칙이 있음 |
| FeatureSpec | `feature/scenario`로 Cucumber 스타일을 모방 | 기능 중심 시나리오를 묶어 읽히게 하고 싶을 때 | 과하게 시나리오화되면 단위 테스트가 무거워질 수 있음 |
| ExpectSpec | `expect("...")`로 정의하고 `context` 중첩 가능 | 기대값 중심으로 테스트를 읽히게 하고 싶을 때 | 기대만 강조하면 “상황/행위” 설명이 부족해질 수 있음 |

### 빠른 선택 팁

- 팀 표준이 없다면: FunSpec
- BDD 문화가 강하면: DescribeSpec 또는 BehaviorSpec
- 시나리오 분류가 복잡하면: FreeSpec

## 공식 문서 링크

- Kotest Framework: `https://kotest.io/docs/framework/framework.html`
- Testing Styles: `https://kotest.io/docs/5.9.x/framework/testing-styles.html`
