package io.github.smling.proxmoxmcpserver.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.smling.proxmoxmcpserver.core.ProxmoxClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeTools extends ProxmoxTool {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public NodeTools(ProxmoxClient proxmox) {
        super(proxmox);
    }

    public String getNodes() {
        try {
            JsonNode result = responseData(proxmox.get("/nodes"));
            List<Map<String, Object>> nodes = new ArrayList<>();
            for (JsonNode node : result) {
                String nodeName = node.path("node").asText();
                try {
                    JsonNode status = responseData(proxmox.get("/nodes/" + nodeName + "/status"));
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("node", nodeName);
                    entry.put("status", node.path("status").asText());
                    entry.put("uptime", status.path("uptime").asLong(0));
                    entry.put("maxcpu", status.path("cpuinfo").path("cpus").asText("N/A"));
                    Map<String, Object> memory = new HashMap<>();
                    memory.put("used", status.path("memory").path("used").asLong(0));
                    memory.put("total", status.path("memory").path("total").asLong(0));
                    entry.put("memory", memory);
                    nodes.add(entry);
                } catch (Exception nodeError) {
                    logger.warn("Using basic info for node {} due to status error", nodeName, nodeError);
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("node", nodeName);
                    entry.put("status", node.path("status").asText());
                    entry.put("uptime", 0);
                    entry.put("maxcpu", "N/A");
                    Map<String, Object> memory = new HashMap<>();
                    memory.put("used", node.path("mem").asLong(0));
                    memory.put("total", node.path("maxmem").asLong(0));
                    entry.put("memory", memory);
                    nodes.add(entry);
                }
            }
            return formatResponse(nodes, "nodes");
        } catch (Exception e) {
            handleError("get nodes", e);
            return "";
        }
    }

    public String getNodeStatus(String nodeName) {
        try {
            JsonNode result = responseData(proxmox.get("/nodes/" + nodeName + "/status"));
            Map<String, Object> status = OBJECT_MAPPER.convertValue(result, Map.class);
            return formatResponse(new NodeStatusPayload(nodeName, status), "node_status");
        } catch (Exception e) {
            try {
                JsonNode nodes = responseData(proxmox.get("/nodes"));
                for (JsonNode entry : nodes) {
                    if (!nodeName.equals(entry.path("node").asText())) {
                        continue;
                    }
                    if ("offline".equalsIgnoreCase(entry.path("status").asText())) {
                        Map<String, Object> fallback = new HashMap<>();
                        fallback.put("status", "offline");
                        fallback.put("uptime", 0);
                        fallback.put("maxcpu", "N/A");
                        Map<String, Object> memory = new HashMap<>();
                        memory.put("used", entry.path("mem").asLong(0));
                        memory.put("total", entry.path("maxmem").asLong(0));
                        fallback.put("memory", memory);
                        return formatResponse(new NodeStatusPayload(nodeName, fallback), "node_status");
                    }
                    break;
                }
            } catch (Exception ignored) {
                handleError("get status for node " + nodeName, e);
            }
            handleError("get status for node " + nodeName, e);
            return "";
        }
    }
}
