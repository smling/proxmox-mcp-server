package io.github.smling.proxmoxmcpserver.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import io.github.smling.proxmoxmcpserver.core.ProxmoxClient;
import io.github.smling.proxmoxmcpserver.formatting.ProxmoxTemplates;
import it.corsinvest.proxmoxve.api.Result;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxmoxTool {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    protected final ProxmoxClient proxmox;
    protected final Logger logger;

    public ProxmoxTool(ProxmoxClient proxmox) {
        this.proxmox = proxmox;
        this.logger = LoggerFactory.getLogger(getClass());
    }

    protected String formatResponse(Object data, String resourceType) {
        if ("nodes".equals(resourceType)) {
            return ProxmoxTemplates.nodeList(castList(data));
        }
        if ("node_status".equals(resourceType)) {
            if (data instanceof NodeStatusPayload payload) {
                return ProxmoxTemplates.nodeStatus(payload.node(), payload.status());
            }
        }
        if ("vms".equals(resourceType)) {
            return ProxmoxTemplates.vmList(castList(data));
        }
        if ("storage".equals(resourceType)) {
            return ProxmoxTemplates.storageList(castList(data));
        }
        if ("containers".equals(resourceType)) {
            return ProxmoxTemplates.containerList(castList(data));
        }
        if ("cluster".equals(resourceType)) {
            return ProxmoxTemplates.clusterStatus(castMap(data));
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return String.valueOf(data);
        }
    }

    protected void handleError(String operation, Exception error) {
        String errorMessage = error.getMessage();
        logger.error("Failed to {}: {}", operation, errorMessage, error);

        if (errorMessage != null) {
            String lower = errorMessage.toLowerCase();
            if (lower.contains("not found")) {
                throw new IllegalArgumentException("Resource not found: " + errorMessage);
            }
            if (lower.contains("permission denied")) {
                throw new IllegalArgumentException("Permission denied: " + errorMessage);
            }
            if (lower.contains("invalid")) {
                throw new IllegalArgumentException("Invalid input: " + errorMessage);
            }
        }
        throw new RuntimeException("Failed to " + operation + ": " + errorMessage, error);
    }

    protected JsonNode responseData(Result result) {
        JsonNode response = result.getResponse();
        if (response == null || response.isNull()) {
            return MissingNode.getInstance();
        }
        return response.path("data");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object data) {
        return (List<Map<String, Object>>) data;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object data) {
        return (Map<String, Object>) data;
    }

    protected record NodeStatusPayload(String node, Map<String, Object> status) {
    }
}
