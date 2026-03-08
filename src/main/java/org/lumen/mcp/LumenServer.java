package org.lumen.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.item.*;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;

public class LumenServer {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(new BufferedReader(new InputStreamReader(System.in)))) {
            System.err.println("Lumen v1.4.1 (God Mode) started. All systems green.");

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.isEmpty()) continue;
                try {
                    JsonNode request = mapper.readTree(line);
                    handleRequest(request);
                } catch (Exception e) {
                    sendError(null, -32700, "Parse error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Critical server error: " + e.getMessage());
        }
    }

    private static void handleRequest(JsonNode request) throws Exception {
        if (!request.has("method")) return;
        String method = request.get("method").asText();
        JsonNode id = request.get("id");

        switch (method) {
            case "initialize": sendInitializeResponse(id); break;
            case "tools/list": sendToolsListResponse(id); break;
            case "tools/call": handleToolCall(id, request.get("params")); break;
            default: if (!method.startsWith("notifications/")) sendError(id, -32601, "Method not found: " + method);
        }
    }

    private static void sendInitializeResponse(JsonNode id) throws Exception {
        ObjectNode result = mapper.createObjectNode();
        result.put("protocolVersion", "2024-11-05");
        result.putObject("capabilities").putObject("tools");
        ObjectNode serverInfo = result.putObject("serverInfo");
        serverInfo.put("name", "lumen-ultimate");
        serverInfo.put("version", "1.4.1");
        sendResponse(id, result);
    }

    private static void sendToolsListResponse(JsonNode id) throws Exception {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode tools = result.putArray("tools");

        addTool(tools, "analyze_exceptions", "예외 및 에러 분석", Map.of("path", "JFR 파일 경로"));
        addTool(tools, "analyze_memory_usage", "메모리/GC 분석", Map.of("path", "JFR 파일 경로"));
        addTool(tools, "analyze_file_io", "디스크 파일 I/O 분석", Map.of("path", "JFR 파일 경로"));
        addTool(tools, "analyze_network_io", "네트워크 통신 분석", Map.of("path", "JFR 파일 경로"));
        addTool(tools, "analyze_jdbc_queries", "SQL 성능 분석", Map.of("path", "JFR 파일 경로"));
        addTool(tools, "analyze_lock_contention", "락 경합 분석", Map.of("path", "JFR 파일 경로"));
        addTool(tools, "analyze_hot_methods", "CPU 핫스팟 분석", Map.of("path", "JFR 파일 경로"));
        addTool(tools, "analyze_os_metrics", "OS 레벨 지표 분석", Map.of("path", "JFR 파일 경로"));
        addTool(tools, "get_jvm_environment", "JVM 환경 확인", Map.of("path", "JFR 파일 경로"));

        sendResponse(id, result);
    }

    private static void addTool(ArrayNode tools, String name, String desc, Map<String, String> params) {
        ObjectNode tool = tools.addObject();
        tool.put("name", name);
        tool.put("description", desc);
        ObjectNode inputSchema = tool.putObject("inputSchema");
        inputSchema.put("type", "object");
        ObjectNode props = inputSchema.putObject("properties");
        for (String key : params.keySet()) props.putObject(key).put("type", "string");
        ArrayNode required = inputSchema.putArray("required");
        for (String key : params.keySet()) required.add(key);
    }

    private static void handleToolCall(JsonNode id, JsonNode params) throws Exception {
        String name = params.get("name").asText();
        JsonNode args = params.get("arguments");
        String path = args.get("path").asText();

        try {
            String report = "";
            switch (name) {
                case "analyze_exceptions": report = runDiagnostic(path, new String[]{"jdk.JavaExceptionThrow", "jdk.JavaErrorThrow"}, "예외 분석", "thrownClass", "message"); break;
                case "analyze_memory_usage": report = analyzeMemory(path); break;
                case "analyze_file_io": report = runDiagnostic(path, new String[]{"jdk.FileRead", "jdk.FileWrite"}, "파일 I/O 분석", "path"); break;
                case "analyze_network_io": report = runDiagnostic(path, new String[]{"jdk.SocketRead", "jdk.SocketWrite"}, "네트워크 분석", "address", "host"); break;
                case "analyze_jdbc_queries": report = runDiagnostic(path, new String[]{"jdk.JDBCQuery"}, "SQL 분석", "query", "sql"); break;
                case "analyze_lock_contention": report = runDiagnostic(path, new String[]{"jdk.JavaMonitorEnter"}, "락 분석", "monitorClass", "class"); break;
                case "analyze_hot_methods": report = runDiagnostic(path, new String[]{"jdk.ExecutionSample"}, "CPU 분석", "stackTrace", "method"); break;
                case "analyze_os_metrics": report = analyzeOS(path); break;
                case "get_jvm_environment": report = runDiagnostic(path, new String[]{"jdk.VMInfo"}, "환경 확인", "jvmVersion", "jvmArguments"); break;
                default: sendError(id, -32601, "Unknown tool"); return;
            }
            ObjectNode result = mapper.createObjectNode();
            result.putArray("content").addObject().put("type", "text").put("text", report);
            sendResponse(id, result);
        } catch (Exception e) {
            sendError(id, -32000, "분석 실패: " + e.getMessage());
        }
    }

    private static String analyzeMemory(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) return "파일 없음: " + filePath;
        IItemCollection items = JfrLoaderToolkit.loadEvents(file);

        IItemCollection gcEvents = items.apply(ItemFilters.type("jdk.GarbageCollection"));
        long totalGcTime = 0;
        int gcCount = 0;
        for (IItemIterable iter : gcEvents) {
            IMemberAccessor<IQuantity, IItem> durAcc = findAccessor(iter.getType(), "duration");
            for (IItem item : iter) {
                totalGcTime += (durAcc != null && durAcc.getMember(item) != null) ? durAcc.getMember(item).longValue() : 0;
                gcCount++;
            }
        }

        IItemCollection heapEvents = items.apply(ItemFilters.type("jdk.GCHeapSummary"));
        long maxUsed = 0;
        for (IItemIterable iter : heapEvents) {
            IMemberAccessor<IQuantity, IItem> usedAcc = findAccessor(iter.getType(), "heapUsed");
            for (IItem item : iter) {
                long used = (usedAcc != null && usedAcc.getMember(item) != null) ? usedAcc.getMember(item).longValue() : 0;
                maxUsed = Math.max(maxUsed, used);
            }
        }

        Map<String, Long> allocations = new HashMap<>();
        IItemCollection allocEvents = items.apply(ItemFilters.or(ItemFilters.type("jdk.ObjectAllocationInNewTLAB"), ItemFilters.type("jdk.ObjectAllocationOutsideTLAB")));
        for (IItemIterable iter : allocEvents) {
            IMemberAccessor<IQuantity, IItem> sizeAcc = findAccessor(iter.getType(), "allocationSize", "size");
            IMemberAccessor<IMCStackTrace, IItem> stAcc = findAccessor(iter.getType(), "stackTrace");
            for (IItem item : iter) {
                long size = (sizeAcc != null && sizeAcc.getMember(item) != null) ? sizeAcc.getMember(item).longValue() : 0;
                String st = (stAcc != null) ? formatStackTrace(stAcc.getMember(item)) : "Unknown";
                allocations.put(st, allocations.getOrDefault(st, 0L) + size);
            }
        }

        StringBuilder sb = new StringBuilder("=== 🧠 Lumen CSI: 메모리 및 GC 분석 ===\n\n");
        sb.append(String.format("📈 [GC 상태] 총 %d회 발생, 누적 정지 시간: %,d ms\n", gcCount, totalGcTime/1_000_000));
        sb.append(String.format("📊 [Heap 사용량] 최대 사용량: %,d MB\n\n", maxUsed / (1024 * 1024)));
        sb.append("💎 [Top 객체 할당 지점]\n");
        allocations.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(5)
            .forEach(e -> sb.append(String.format("- %s: %,d KB\n", e.getKey(), e.getValue()/1024)));
        return sb.toString();
    }

    private static String analyzeOS(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) return "파일 없음: " + filePath;
        IItemCollection items = JfrLoaderToolkit.loadEvents(file);

        IItemCollection cpuLoadEvents = items.apply(ItemFilters.type("jdk.CPULoad"));
        double maxSystemLoad = 0;
        double maxJvmLoad = 0;
        for (IItemIterable iter : cpuLoadEvents) {
            IMemberAccessor<IQuantity, IItem> sysAcc = findAccessor(iter.getType(), "system");
            IMemberAccessor<IQuantity, IItem> jvmAcc = findAccessor(iter.getType(), "jvmUser", "user");
            for (IItem item : iter) {
                if (sysAcc != null && sysAcc.getMember(item) != null) maxSystemLoad = Math.max(maxSystemLoad, sysAcc.getMember(item).doubleValue());
                if (jvmAcc != null && jvmAcc.getMember(item) != null) maxJvmLoad = Math.max(maxJvmLoad, jvmAcc.getMember(item).doubleValue());
            }
        }

        IItemCollection swEvents = items.apply(ItemFilters.type("jdk.ThreadContextSwitchRate"));
        double maxSwRate = 0;
        for (IItemIterable iter : swEvents) {
            IMemberAccessor<IQuantity, IItem> rateAcc = findAccessor(iter.getType(), "switchRate", "rate");
            for (IItem item : iter) {
                if (rateAcc != null && rateAcc.getMember(item) != null) maxSwRate = Math.max(maxSwRate, rateAcc.getMember(item).doubleValue());
            }
        }

        StringBuilder sb = new StringBuilder("=== 🐧 Lumen CSI: OS 지표 분석 ===\n\n");
        sb.append(String.format("🖥️ [CPU 로드] JVM 점유율: %.1f%%, 전체 시스템: %.1f%%\n", maxJvmLoad * 100, maxSystemLoad * 100));
        sb.append(String.format("🔄 [컨텍스트 스위칭] 최대 발생률: %.1f switches/s\n", maxSwRate));
        return sb.toString();
    }

    private static String runDiagnostic(String filePath, String[] typeIds, String title, String... keywords) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) return "파일 없음: " + filePath;
        IItemCollection items = JfrLoaderToolkit.loadEvents(file);
        
        List<IItemFilter> filters = new ArrayList<>();
        for (String tid : typeIds) filters.add(ItemFilters.type(tid));
        IItemCollection targetEvents = items.apply(ItemFilters.or(filters.toArray(new IItemFilter[0])));
        
        Map<String, Long> locationDurations = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();

        for (IItemIterable iter : targetEvents) {
            IMemberAccessor<IQuantity, IItem> durAcc = findAccessor(iter.getType(), "duration");
            IMemberAccessor<IMCStackTrace, IItem> stAcc = findAccessor(iter.getType(), "stackTrace");
            IMemberAccessor<?, IItem> dataAcc = findAccessor(iter.getType(), keywords);

            for (IItem item : iter) {
                long ns = (durAcc != null && durAcc.getMember(item) != null) ? durAcc.getMember(item).longValue() : 0;
                
                // [개선] 지정된 키워드 외에도 유의미한 텍스트 속성을 자동으로 수집
                StringBuilder dataBuilder = new StringBuilder();
                Set<String> collectedVals = new HashSet<>();
                
                // 1. 우선순위 키워드 수집
                for (String kw : keywords) {
                    IMemberAccessor<?, IItem> acc = findAccessor(iter.getType(), kw);
                    Object val = (acc != null) ? acc.getMember(item) : null;
                    if (val != null && !val.toString().isEmpty() && !val.toString().equals("N/A")) {
                        String sVal = val.toString();
                        if (!collectedVals.contains(sVal)) {
                            if (dataBuilder.length() > 0) dataBuilder.append(" | ");
                            dataBuilder.append(sVal);
                            collectedVals.add(sVal);
                        }
                    }
                }
                
                // 2. 만약 수집된게 너무 적으면 모든 속성을 뒤져서 텍스트를 찾음 (Java 8 호환성)
                if (dataBuilder.length() < 5) {
                    for (IAttribute<?> attr : iter.getType().getAttributes()) {
                        IMemberAccessor<?, IItem> acc = iter.getType().getAccessor(attr.getKey());
                        Object val = acc.getMember(item);
                        if (val instanceof String && !val.toString().isEmpty() && !val.toString().contains("stackTrace")) {
                            String sVal = val.toString();
                            if (!collectedVals.contains(sVal)) {
                                if (dataBuilder.length() > 0) dataBuilder.append(" : ");
                                dataBuilder.append(sVal);
                                collectedVals.add(sVal);
                            }
                        }
                    }
                }
                
                String data = dataBuilder.length() > 0 ? dataBuilder.toString() : "N/A";
                String st = (stAcc != null) ? formatStackTrace(stAcc.getMember(item)) : "";
                String key = (iter.getType().getIdentifier().contains("FileWrite") ? "[WRITE] " : "") + data + (st.isEmpty() ? "" : " at " + st);
                locationDurations.put(key, locationDurations.getOrDefault(key, 0L) + ns);
                counts.put(key, counts.getOrDefault(key, 0) + 1);
            }
        }
        StringBuilder sb = new StringBuilder("=== 🔦 Lumen CSI: " + title + " ===\n\n");
        if (counts.isEmpty()) return sb.append("✅ 감지된 데이터가 없습니다.").toString();
        locationDurations.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(10)
            .forEach(e -> sb.append(String.format("- %s\n  누적 지연: %,d ms (총 %d회)\n\n", e.getKey(), e.getValue()/1_000_000, counts.get(e.getKey()))));
        return sb.toString();
    }

    private static IMemberAccessor findAccessor(IType<IItem> type, String... possibleNames) {
        for (String name : possibleNames) {
            for (IAttribute<?> attr : type.getAttributes()) {
                if (attr.getIdentifier().toLowerCase().contains(name.toLowerCase())) return (IMemberAccessor) type.getAccessor(attr.getKey());
            }
        }
        return null;
    }

    private static String formatStackTrace(IMCStackTrace st) {
        if (st == null || st.getFrames() == null || st.getFrames().isEmpty()) return "";
        List<? extends IMCFrame> frames = st.getFrames();
        
        // [수정] 필터링 없이 최상단 프레임 정보를 정직하게 출력
        IMCFrame topFrame = frames.get(0);
        String cn = topFrame.getMethod().getType().getFullName();
        Integer lineNum = topFrame.getFrameLineNumber();
        String current = String.format("%s.%s(Line:%s)", cn, topFrame.getMethod().getMethodName(), (lineNum != null ? lineNum : "Native"));
        
        // 호출자 정보 한 단계 추가 (맥락 파악용)
        if (frames.size() > 1) {
            IMCFrame caller = frames.get(1);
            String ccn = caller.getMethod().getType().getFullName();
            return current + " <- " + ccn + "." + caller.getMethod().getMethodName();
        }
        return current;
    }

    private static void sendResponse(JsonNode id, ObjectNode result) throws Exception {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        response.set("result", result);
        System.out.println(mapper.writeValueAsString(response));
        System.out.flush();
    }

    private static void sendError(JsonNode id, int code, String message) throws Exception {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        ObjectNode error = response.putObject("error");
        error.put("code", code);
        error.put("message", message);
        System.out.println(mapper.writeValueAsString(response));
        System.out.flush();
    }
}
