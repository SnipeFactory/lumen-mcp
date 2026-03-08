# 🧠 SnipeFactory: Lumen MCP Engine

[![English](https://img.shields.io/badge/Language-English-blue.svg)](./README.md)
[![언어: 한글](https://img.shields.io/badge/언어-한글-red.svg)](#)

> **JVM Flight Recorder (JFR) Forensic Engine for LLM**  
> **하이브리드 TS/Java 기반 JFR 정밀 분석 엔진: 제미나이용 루멘 MCP 서버**

Lumen MCP는 루멘 생태계의 분석 두뇌입니다. 고성능 자바 엔진을 사용하여 JFR 파일을 파싱하고, TypeScript 브릿지를 통해 제미나이(Gemini)나 클로드(Claude)와 같은 AI 에이전트에게 최적화된 MCP 인터페이스를 제공합니다.

---

## ✨ 주요 기능

- **하이브리드 지능:** 자바의 강력한 JFR 파싱 능력과 Node.js의 폭넓은 생태계 호환성을 결합했습니다.
- **예외 분석 (Exception Forensics):** 표준 로그에 찍히지 않는 숨겨진 예외(Swallowed Exceptions)와 전체 호출 체인을 추적합니다.
- **SQL 및 I/O 분석:** 슬로우 쿼리와 소켓 지연 지점을 코드 라인 단위로 정확히 짚어냅니다.
- **Smithery 지원:** Smithery CLI와 완벽하게 호환되어 설치와 배포가 매우 간편합니다.

---

## 🚀 설치 및 설정 (Installation)

### 1. 프로젝트 빌드
Lumen MCP는 Node.js(18+)와 Java(11+) 환경이 모두 필요합니다.
```bash
npm install
npm run build
```
*(이 명령은 브릿지용 `tsc`와 엔진용 `./gradlew installDist`를 순차적으로 실행합니다.)*

### 2. Gemini CLI 등록
이제 Node.js 브릿지를 통해 루멘을 실행하는 것을 권장합니다.
```json
{
  "mcpServers": {
    "lumen": {
      "command": "node",
      "args": [
        "/path/to/lumen-mcp/dist/server.js"
      ]
    }
  }
}
```

---

## 🔍 제공 도구 (Diagnostic Tools)

| 도구명 | 설명 |
|---|---|
| `analyze_exceptions` | 예외 발생 지점 및 호출 체인(부모 메서드) 분석 |
| `analyze_jdbc_queries` | 슬로우 SQL 쿼리 및 소속 코드 분석 (Java 11+) |
| `analyze_network_io` | 소켓 통신 지연 분석 (Java 8 환경 필수 도구) |
| `analyze_hot_methods` | CPU를 가장 많이 사용하는 코드 핫스팟 탐지 |
| `analyze_memory_usage` | GC 상태 및 객체 할당 지점 분석 |
| `analyze_lock_contention` | 스레드 동기화 병목 지점 분석 |

---

## 📖 사용 예시

제미나이에게 직접 수사를 맡기세요.
> **"제미나이야, 루멘을 써서 `data/crash.jfr` 분석해줘. 어느 쿼리가 DB 응답을 느리게 만드는지 알려줘."**

---
© 2026 [SnipeFactory](https://github.com/SnipeFactory). All rights reserved.
