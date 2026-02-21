package io.github.smling.proxmoxmcpserver.tools.console;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import io.github.smling.proxmoxmcpserver.core.ProxmoxClient;
import it.corsinvest.proxmoxve.api.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class VmConsoleManager {
    private final ProxmoxClient proxmox;
    private static final Logger logger = LoggerFactory.getLogger(VmConsoleManager.class);

    public VmConsoleManager(ProxmoxClient proxmox) {
        this.proxmox = proxmox;
    }

    public Map<String, Object> executeCommand(String node, String vmid, String command) {
        try {
            JsonNode status = responseData(proxmox.get("/nodes/" + node + "/qemu/" + vmid + "/status/current"));
            if (!"running".equalsIgnoreCase(status.path("status").asText())) {
                logger.error("Failed to execute command on VM {}: VM is not running", vmid);
                throw new IllegalArgumentException("VM " + vmid + " on node " + node + " is not running");
            }

            logger.info("Executing command on VM {} (node: {}): {}", vmid, node, command);
            JsonNode execResult = responseData(proxmox.postForm(
                "/nodes/" + node + "/qemu/" + vmid + "/agent/exec",
                Map.of("command", command)
            ));

            JsonNode pidNode = execResult.get("pid");
            if (pidNode == null || pidNode.isNull()) {
                throw new IllegalStateException("No PID returned from command execution");
            }

            Thread.sleep(1000);

            JsonNode console = responseData(proxmox.get(
                "/nodes/" + node + "/qemu/" + vmid + "/agent/exec-status",
                Map.of("pid", pidNode.asText())
            ));

            Map<String, Object> response = new HashMap<>();
            if (console != null && console.isObject()) {
                response.put("success", true);
                response.put("output", console.path("out-data").asText(""));
                response.put("error", console.path("err-data").asText(""));
                response.put("exit_code", console.path("exitcode").asInt(0));
                return response;
            }

            response.put("success", true);
            response.put("output", console == null ? "" : console.toString());
            response.put("error", "");
            response.put("exit_code", 0);
            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to execute command on VM {}", vmid, e);
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("not found")) {
                throw new IllegalArgumentException("VM " + vmid + " not found on node " + node);
            }
            throw new RuntimeException("Failed to execute command: " + e.getMessage(), e);
        }
    }

    private JsonNode responseData(Result result) {
        JsonNode response = result.getResponse();
        if (response == null || response.isNull()) {
            return MissingNode.getInstance();
        }
        return response.path("data");
    }
}
