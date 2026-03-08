# 🧠 SnipeFactory: Lumen MCP Engine

[![Language: English](https://img.shields.io/badge/Language-English-blue.svg)](#)
[![한글](https://img.shields.io/badge/언어-한글-red.svg)](./README.ko.md)

> **JVM Flight Recorder (JFR) Forensic Engine for LLM**  
> **Advanced JFR Analysis Engine: Lumen MCP Server for Gemini & LLMs**

Lumen MCP is the analytical brain of the Lumen ecosystem. It parses complex JVM Flight Recorder (JFR) binary files and translates them into actionable insights, enabling LLMs to pinpoint precise root causes of Java server incidents.

---

## ✨ Key Features

- **Exception Forensics:** Trace hidden exceptions (Swallowed Exceptions) that never appear in standard application logs.
- **SQL Analytics:** Analyze slow query execution times and pinpoint the exact DAO/Mapper source (Supports Java 11+).
- **Resource Profiling:** Detect CPU Hotspots, Memory Leaks, and Lock Contention with line-level precision.
- **Generic & Unbiased:** A standalone engine that works with any Java project without requiring code changes.

---

## 🚀 Installation & Setup

Lumen MCP implements the Model Context Protocol (MCP) and integrates seamlessly with clients like Gemini CLI.

### 1. Build the Project
```bash
./gradlew installDist
```

### 2. Register with Gemini CLI
Add the following configuration to your `.gemini/settings.json` file:
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

Once registered, Lumen provides the following tools to your LLM agent:

| Tool | Description |
|---|---|
| `analyze_exceptions` | Analyzes exception throw points and call chains. |
| `analyze_jdbc_queries` | Identifies slow SQL queries and their source lines (Java 11+). |
| `analyze_network_io` | Detects socket latency (Essential for Java 8 DB/API triage). |
| `analyze_hot_methods` | Pinpoints code lines consuming the most CPU cycles. |
| `analyze_memory_usage` | Analyzes GC health and object allocation hotspots. |
| `analyze_lock_contention` | Identifies thread synchronization bottlenecks. |
| `analyze_os_metrics` | Reports OS-level load, context switching, and system health. |

---

## 📖 Usage Example

Simply ask your LLM agent to investigate a JFR file:
> **"Gemini, analyze `data/incident_report.jfr`. Tell me why the server response time spiked at 3 PM yesterday."**

---
© 2026 [SnipeFactory](https://github.com/SnipeFactory). Part of the Lumen Ecosystem.
