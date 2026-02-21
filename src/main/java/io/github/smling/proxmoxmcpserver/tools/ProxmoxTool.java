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

/**
 * Base class for Proxmox tool implementations.
 */
public class ProxmoxTool {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    protected final ProxmoxClient proxmox;
    protected final Logger logger;

    /**
     * Creates a tool with a configured Proxmox client.
     *
     * @param proxmox the Proxmox client
     */
    public ProxmoxTool(ProxmoxClient proxmox) {
        this.proxmox = proxmox;
        this.logger = LoggerFactory.getLogger(getClass());
    }

    /**
     * Formats a response payload using templates or JSON pretty printing.
     *
     * @param data the payload to format
     * @param resourceType the resource type identifier
     * @return formatted output string
     */
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

    /**
     * Logs and throws an exception with a friendly message.
     *
     * @param operation the operation name
     * @param error the encountered error
     */
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

    /**
     * Extracts the data section from a Proxmox API response.
     *
     * @param result the Proxmox API result
     * @return the data node or a missing node
     */
    protected JsonNode responseData(Result result) {
        JsonNode response = result.getResponse();
        if (response == null || response.isNull()) {
            return MissingNode.getInstance();
        }
        return response.path("data");
    }

    /**
     * Casts an object to a list of maps.
     *
     * @param data the object to cast
     * @return the list of maps
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object data) {
        return (List<Map<String, Object>>) data;
    }

    /**
     * Casts an object to a map.
     *
     * @param data the object to cast
     * @return the map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object data) {
        return (Map<String, Object>) data;
    }

    /**
     * Payload wrapper for node status responses.
     *
     * @param node the node name
     * @param status the node status payload
     */
    protected record NodeStatusPayload(String node, Map<String, Object> status) {
    }
}
