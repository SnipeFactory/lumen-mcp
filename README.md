# 🧠 SnipeFactory: Lumen MCP Engine

> **JVM Flight Recorder (JFR) Forensic Engine for LLM**  
> **LLM 기반 JFR 정밀 분석 엔진: 제미나이용 루멘 MCP 서버**

Lumen MCP는 JVM Flight Recorder(JFR) 바이너리 파일을 낱낱이 파헤쳐, LLM(제미나이 등)이 자바 서버의 병목과 장애 원인을 정확히 짚어낼 수 있도록 돕는 핵심 분석 엔진입니다.

---

## ✨ Key Features

- **Exception Forensic:** 로그에 남지 않은 숨겨진 예외(Exception Swallow)까지 추적.
- **SQL Analytics:** 슬로우 쿼리의 실행 시간 및 발생 지점 정밀 분석 (Java 11+ 지원).
- **Resource Profiling:** CPU Hotspot, Memory Leak, Lock Contention 감지.
- **Generic Engine:** 특정 프로젝트에 의존하지 않는 범용 자바 분석 도구.

---

## 🚀 Installation & Setup

Lumen MCP는 Model Context Protocol(MCP)을 준수하며, Gemini CLI와 같은 MCP 클라이언트에서 바로 사용할 수 있습니다.

### 1. 프로젝트 빌드
```bash
./gradlew installDist
```

### 2. Gemini CLI 등록
`.gemini/settings.json` 파일에 아래 설정을 추가합니다.
```json
{
  "mcpServers": {
    "lumen": {
      "command": "java",
      "args": [
        "-cp",
        "/path/to/lumen-mcp/build/install/lumen-mcp/lib/*",
        "org.lumen.mcp.LumenServer"
      ]
    }
  }
}
```

---

## 🔍 Diagnostic Tools

등록된 루멘은 아래와 같은 강력한 도구들을 제공합니다.

| Tool | Description |
|---|---|
| `analyze_exceptions` | 예외 발생 지점 및 호출 체인 분석 |
| `analyze_jdbc_queries` | 슬로우 쿼리 및 실행 지점 분석 (Java 11+) |
| `analyze_network_io` | 소켓 통신 지연 분석 (Java 8 필수 도구) |
| `analyze_hot_methods` | CPU를 가장 많이 점유하는 코드 라인 분석 |
| `analyze_memory_usage` | GC 상태 및 객체 할당 지점 분석 |
| `analyze_lock_contention` | 스레드 간 락 경합 발생 지점 분석 |
| `analyze_os_metrics` | OS 레벨의 부하 및 컨텍스트 스위칭 분석 |

---

## 📖 Usage Example

제미나이에게 분석을 요청하세요.
> **"제미나이야, `data/crash.jfr` 파일 분석해줘. 왜 어제 저녁에 서버가 느려졌는지 알고 싶어."**

---
© 2026 [SnipeFactory](https://github.com/SnipeFactory). Part of the Lumen Ecosystem.
