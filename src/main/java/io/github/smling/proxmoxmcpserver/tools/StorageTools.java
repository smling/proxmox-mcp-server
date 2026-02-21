package io.github.smling.proxmoxmcpserver.tools;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.smling.proxmoxmcpserver.core.ProxmoxClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Storage-related Proxmox operations.
 */
public class StorageTools extends ProxmoxTool {
    /**
     * Creates storage tools with a Proxmox client.
     *
     * @param proxmox the Proxmox client
     */
    public StorageTools(ProxmoxClient proxmox) {
        super(proxmox);
    }

    /**
     * Lists storage pools with usage metrics.
     *
     * @return formatted storage list
     */
    public String getStorage() {
        try {
            JsonNode result = responseData(proxmox.get("/storage"));
            List<Map<String, Object>> storage = new ArrayList<>();

            for (JsonNode store : result) {
                String storageName = store.path("storage").asText();
                String node = store.path("node").asText("localhost");
                try {
                    JsonNode status = responseData(
                        proxmox.get("/nodes/" + node + "/storage/" + storageName + "/status")
                    );
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("storage", storageName);
                    entry.put("type", store.path("type").asText());
                    entry.put("content", store.path("content").asText());
                    entry.put("status", store.path("enabled").asBoolean(true) ? "online" : "offline");
                    entry.put("used", status.path("used").asLong(0));
                    entry.put("total", status.path("total").asLong(0));
                    entry.put("available", status.path("avail").asLong(0));
                    storage.add(entry);
                } catch (Exception storeError) {
                    logger.warn("Using basic info for storage {} due to status error", storageName, storeError);
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("storage", storageName);
                    entry.put("type", store.path("type").asText());
                    entry.put("content", store.path("content").asText());
                    entry.put("status", store.path("enabled").asBoolean(true) ? "online" : "offline");
                    entry.put("used", 0L);
                    entry.put("total", 0L);
                    entry.put("available", 0L);
                    storage.add(entry);
                }
            }

            return formatResponse(storage, "storage");
        } catch (Exception e) {
            handleError("get storage", e);
            return "";
        }
    }
}
