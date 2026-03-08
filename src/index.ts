import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import { spawn } from "child_process";
import path from "path";
import { fileURLToPath } from "url";

/**
 * [Lumen Hybrid Bridge]
 * Smithery의 스캐너 호환성을 위해 TypeScript 진입점을 제공하면서,
 * 실제 무거운 분석은 고성능 Java 엔진에 위임합니다.
 */

// Smithery 스캔 시 import.meta.url이 없을 경우를 대비한 안전한 경로 계산
const getDirname = () => {
  try {
    return path.dirname(fileURLToPath(import.meta.url));
  } catch (e) {
    return process.cwd();
  }
};

const __dirname = getDirname();
const JAVA_CP = path.join(__dirname, "../build/install/lumen-mcp/lib/*");
const JAVA_CLASS = "org.lumen.mcp.LumenServer";

/**
 * Smithery 스캐너가 도구 목록을 파악할 수 있도록 서버 인스턴스 생성 함수 노출
 */
export function createLumenServer() {
  const server = new Server(
    {
      name: "lumen-ultimate",
      version: "1.4.5",
    },
    {
      capabilities: {
        tools: {},
      },
    }
  );

  // 스캐너를 위한 도구 명세 정의
  server.setRequestHandler(ListToolsRequestSchema, async () => {
    return {
      tools: [
        {
          name: "analyze_exceptions",
          description: "Analyze JFR exception events and call chains",
          inputSchema: {
            type: "object",
            properties: { path: { type: "string" } },
            required: ["path"],
          },
        },
        {
          name: "analyze_jdbc_queries",
          description: "Analyze slow SQL queries (Java 11+)",
          inputSchema: {
            type: "object",
            properties: { path: { type: "string" } },
            required: ["path"],
          },
        },
        {
          name: "analyze_network_io",
          description: "Analyze socket latency (Essential for Java 8 triage)",
          inputSchema: {
            type: "object",
            properties: { path: { type: "string" } },
            required: ["path"],
          },
        },
        {
          name: "analyze_hot_methods",
          description: "Pinpoint CPU hotspots in code with stack trace",
          inputSchema: {
            type: "object",
            properties: { path: { type: "string" } },
            required: ["path"],
          },
        },
        {
          name: "analyze_memory_usage",
          description: "Analyze GC health and object allocation hotspots",
          inputSchema: {
            type: "object",
            properties: { path: { type: "string" } },
            required: ["path"],
          },
        },
        {
          name: "analyze_lock_contention",
          description: "Identify thread synchronization bottlenecks",
          inputSchema: {
            type: "object",
            properties: { path: { type: "string" } },
            required: ["path"],
          },
        },
      ],
    };
  });

  // 도구 호출은 실제 실행 시 Proxy 모드에서 처리하므로 여기서는 최소한의 응답만 제공
  server.setRequestHandler(CallToolRequestSchema, async () => {
    return {
      content: [{ type: "text", text: "Java engine is required for execution." }],
    };
  });

  return server;
}

/**
 * Smithery 전용 샌드박스 서버 노출 (필수)
 */
export function createSandboxServer() {
  return createLumenServer();
}

/**
 * 실제 실행 시에는 Java 프로세스로 통째로 파이핑하여 성능 손실을 방지합니다.
 */
async function runProxy() {
  const java = spawn("java", ["-cp", JAVA_CP, JAVA_CLASS], {
    stdio: ["pipe", "pipe", "inherit"],
  });

  process.stdin.pipe(java.stdin);
  java.stdout.pipe(process.stdout);

  java.on("exit", (code) => {
    process.exit(code || 0);
  });

  java.on("error", (err) => {
    console.error("[Lumen] Failed to start Java engine:", err);
    process.exit(1);
  });
}

// 직접 실행될 때만 Proxy 모드 가동
const isMain = process.argv[1] && (
  process.argv[1].endsWith('index.js') || 
  process.argv[1].endsWith('server.js')
);

if (isMain) {
  runProxy().catch(console.error);
}
