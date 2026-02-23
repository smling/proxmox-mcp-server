package io.github.smling.proxmoxmcpserver.tools;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.smling.proxmoxmcpserver.core.ProxmoxClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cluster-related Proxmox operations.
 */
public class ClusterTools extends ProxmoxTool {
    /**
     * Creates cluster tools with a Proxmox client.
     *
     * @param proxmox the Proxmox client
     */
    public ClusterTools(ProxmoxClient proxmox) {
        super(proxmox);
    }

    /**
     * Returns cluster status details and resource counts.
     *
     * @return formatted cluster status
     */
    public String getClusterStatus() {
        try {
            JsonNode result = responseData(proxmox.get("/cluster/status"));
            JsonNode firstItem = !result.isEmpty() ? result.get(0) : null;
            Map<String, Object> status = new HashMap<>();
            status.put("name", firstItem == null ? null : firstItem.path("name").asText(null));
            status.put("quorum", firstItem != null && firstItem.path("quorate").asBoolean(false));

            int nodeCount = 0;
            List<Map<String, Object>> resources = new ArrayList<>();
            for (JsonNode entry : result) {
                String type = entry.path("type").asText();
                if ("node".equals(type)) {
                    nodeCount++;
                } else if ("resource".equals(type)) {
                    resources.add(Map.of(
                        "type", entry.path("id").asText(),
                        "status", entry.path("status").asText()
                    ));
                }
            }
            status.put("nodes", nodeCount);
            status.put("resources", resources);
            return formatResponse(status, "cluster");
        } catch (Exception e) {
            handleError("get cluster status", e);
            return "";
        }
    }
}
