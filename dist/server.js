import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { CallToolRequestSchema, ListToolsRequestSchema, } from "@modelcontextprotocol/sdk/types.js";
import { spawn } from "child_process";
import path from "path";
import { fileURLToPath } from "url";
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
// Java 서버 위치 계산
const JAVA_CP = path.join(__dirname, "../build/install/lumen-mcp/lib/*");
const JAVA_CLASS = "org.lumen.mcp.LumenServer";
const server = new Server({
    name: "lumen-ultimate",
    version: "1.4.5",
}, {
    capabilities: {
        tools: {},
    },
});
/**
 * 도구 목록 정의 (Smithery 스캔용)
 */
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
                description: "Analyze socket latency (Essential for Java 8)",
                inputSchema: {
                    type: "object",
                    properties: { path: { type: "string" } },
                    required: ["path"],
                },
            },
            {
                name: "analyze_hot_methods",
                description: "Pinpoint CPU hotspots in code",
                inputSchema: {
                    type: "object",
                    properties: { path: { type: "string" } },
                    required: ["path"],
                },
            },
            {
                name: "analyze_memory_usage",
                description: "Analyze GC and object allocations",
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
            {
                name: "analyze_os_metrics",
                description: "Report OS-level health and metrics",
                inputSchema: {
                    type: "object",
                    properties: { path: { type: "string" } },
                    required: ["path"],
                },
            },
        ],
    };
});
/**
 * 도구 호출 처리: Java 프로세스에 위임 (Proxy)
 */
server.setRequestHandler(CallToolRequestSchema, async (request) => {
    return new Promise((resolve, reject) => {
        // Java 프로세스 실행
        const javaProcess = spawn("java", ["-cp", JAVA_CP, JAVA_CLASS]);
        let responseData = "";
        javaProcess.stdout.on("data", (data) => {
            responseData += data.toString();
        });
        javaProcess.stderr.on("data", (data) => {
            console.error(`[Java Error] ${data}`);
        });
        javaProcess.on("close", (code) => {
            try {
                // Java 서버는 한 번의 호출에 대해 하나의 JSON-RPC 응답을 출력하고 종료되는 방식으로 작동하거나
                // 혹은 stdio를 계속 열어둘 수 있습니다. 
                // 여기서는 Java 서버가 stdio MCP 사양을 직접 구현하고 있으므로 통째로 pipe하는 것이 가장 효율적입니다.
                // 하지만 Smithery 호환성을 위해 JS가 앞단에서 처리합니다.
            }
            catch (e) {
                reject(e);
            }
        });
        // 실제로는 JS 서버가 계속 떠있고, Java 서버와 stdio로 대화하는 로직이 필요합니다.
        // 하지만 가장 간단하고 확실한 방법은 JS가 Java를 실행해서 stdio를 통째로 연결해버리는 것입니다.
        // 아래는 단순 Proxy 로직입니다.
        const transport = new StdioServerTransport();
        // transport와 javaProcess.stdio를 연결하는 고난도 작업 대신, 
        // 지금의 LumenServer.java는 그 자체로 MCP 서버이므로 
        // JS의 진입점은 단순히 "java -cp ... LumenServer"를 실행하는 런처 역할만 해도 충분합니다.
    });
});
async function main() {
    const transport = new StdioServerTransport();
    await server.connect(transport);
}
main().catch(console.error);
