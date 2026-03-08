# 🧠 SnipeFactory: Lumen MCP Engine

[![Language: English](https://img.shields.io/badge/Language-English-blue.svg)](#)
[![한글](https://img.shields.io/badge/언어-한글-red.svg)](./README.ko.md)

> **JVM Flight Recorder (JFR) Forensic Engine for LLM**  
> **Hybrid TypeScript/Java MCP Server for Advanced JVM Analysis**

Lumen MCP is the analytical brain of the Lumen ecosystem. It leverages a high-performance Java engine to parse JFR files and a TypeScript bridge to provide a seamless Model Context Protocol (MCP) interface for AI agents like Gemini and Claude.

---

## ✨ Key Features

- **Hybrid Intelligence:** Combines Java's deep JFR parsing power with Node.js's ecosystem compatibility.
- **Exception Forensics:** Trace hidden exceptions (Swallowed Exceptions) with full call chains.
- **SQL & I/O Analytics:** Pinpoint slow queries and socket latencies with line-level precision.
- **Smithery Ready:** Fully compatible with Smithery CLI for instant distribution.

---

## 🚀 Installation & Setup

### 1. Build the Project
Lumen MCP requires both Node.js (18+) and Java (11+) to build.
```bash
npm install
npm run build
```
*(This command runs `tsc` for the bridge and `./gradlew installDist` for the engine.)*

### 2. Register with Gemini CLI
You can now run Lumen using the Node.js bridge (Recommended):
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

## 🔍 Diagnostic Tools

| Tool | Description |
|---|---|
| `analyze_exceptions` | Analyzes exception throw points and call chains. |
| `analyze_jdbc_queries` | Identifies slow SQL queries and their source lines (Java 11+). |
| `analyze_network_io` | Detects socket latency (Essential for Java 8 triage). |
| `analyze_hot_methods` | Pinpoints code lines consuming the most CPU cycles. |
| `analyze_memory_usage` | Analyzes GC health and object allocation hotspots. |
| `analyze_lock_contention` | Identifies thread synchronization bottlenecks. |

---

## 📖 Usage Example

> **"Gemini, use lumen to analyze `data/incident.jfr`. Tell me why the database response is slow."**

---
© 2026 [SnipeFactory](https://github.com/SnipeFactory). All rights reserved.
