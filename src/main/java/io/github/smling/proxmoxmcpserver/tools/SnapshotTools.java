package io.github.smling.proxmoxmcpserver.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.smling.proxmoxmcpserver.core.ProxmoxClient;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Snapshot-related Proxmox operations for VMs and containers.
 */
public class SnapshotTools extends ProxmoxTool {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    /**
     * Creates snapshot tools with a Proxmox client.
     *
     * @param proxmox the Proxmox client
     */
    public SnapshotTools(ProxmoxClient proxmox) {
        super(proxmox);
    }

    /**
     * Lists snapshots for a VM or container.
     *
     * @param node target node name
     * @param vmid VM or container ID
     * @param vmType VM type (qemu or lxc)
     * @return formatted snapshot list
     */
    public String listSnapshots(String node, String vmid, String vmType) {
        try {
            JsonNode snapshots = "lxc".equalsIgnoreCase(vmType)
                ? responseData(proxmox.get("/nodes/" + node + "/lxc/" + vmid + "/snapshot"))
                : responseData(proxmox.get("/nodes/" + node + "/qemu/" + vmid + "/snapshot"));

            if (!snapshots.isArray() || snapshots.isEmpty()) {
                return "No snapshots found for " + vmType.toUpperCase() + " " + vmid + " on node " + node;
            }

            StringBuilder builder = new StringBuilder("Snapshots for ")
                .append(vmType.toUpperCase()).append(" ").append(vmid).append(" on ").append(node).append("\n");

            for (JsonNode snap : snapshots) {
                String name = snap.path("name").asText("unknown");
                if ("current".equals(name)) {
                    continue;
                }
                builder.append("\n  ").append(name).append("\n");
                if (snap.hasNonNull("description")) {
                    builder.append("     Description: ").append(snap.path("description").asText()).append("\n");
                }
                if (snap.hasNonNull("snaptime")) {
                    long snapTime = snap.path("snaptime").asLong(0);
                    builder.append("     Created: ")
                        .append(snapTime > 0 ? TIME_FORMAT.format(Instant.ofEpochSecond(snapTime))
                            : snap.path("snaptime").asText())
                        .append("\n");
                }
                if (snap.hasNonNull("parent")) {
                    builder.append("     Parent: ").append(snap.path("parent").asText()).append("\n");
                }
                if (snap.path("vmstate").asBoolean(false)) {
                    builder.append("     RAM State: Included\n");
                }
            }

            return builder.toString().trim();
        } catch (Exception e) {
            return errorPayload("list snapshots for " + vmType + " " + vmid, e);
        }
    }

    /**
     * Creates a snapshot for a VM or container.
     *
     * @param node target node name
     * @param vmid VM or container ID
     * @param snapname snapshot name
     * @param description snapshot description
     * @param vmstate include memory state for VMs
     * @param vmType VM type (qemu or lxc)
     * @return snapshot creation status
     */
    public String createSnapshot(
        String node,
        String vmid,
        String snapname,
        String description,
        boolean vmstate,
        String vmType
    ) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("snapname", snapname);
            if (description != null && !description.isBlank()) {
                params.put("description", description);
            }
            if (!"lxc".equalsIgnoreCase(vmType) && vmstate) {
                params.put("vmstate", "1");
            }

            JsonNode result = "lxc".equalsIgnoreCase(vmType)
                ? responseData(proxmox.postForm("/nodes/" + node + "/lxc/" + vmid + "/snapshot", params))
                : responseData(proxmox.postForm("/nodes/" + node + "/qemu/" + vmid + "/snapshot", params));

            StringBuilder builder = new StringBuilder("Snapshot Created Successfully\n\n");
            builder.append("  Name: ").append(snapname).append("\n");
            builder.append("  ").append(vmType.toUpperCase()).append(" ID: ").append(vmid).append("\n");
            builder.append("  Node: ").append(node).append("\n");
            if (description != null && !description.isBlank()) {
                builder.append("  Description: ").append(description).append("\n");
            }
            if (vmstate && !"lxc".equalsIgnoreCase(vmType)) {
                builder.append("  RAM State: Included\n");
            }
            builder.append("\nTask ID: ").append(result).append("\n\n")
                .append("Next steps:\n")
                .append("  - List snapshots: listSnapshots node='").append(node)
                .append("' vmid='").append(vmid).append("' vmType='").append(vmType).append("'\n")
                .append("  - Rollback: rollbackSnapshot node='").append(node)
                .append("' vmid='").append(vmid).append("' snapname='").append(snapname)
                .append("' vmType='").append(vmType).append("'");

            return builder.toString();
        } catch (Exception e) {
            return errorPayload("create snapshot '" + snapname + "' for " + vmType + " " + vmid, e);
        }
    }

    /**
     * Deletes a snapshot for a VM or container.
     *
     * @param node target node name
     * @param vmid VM or container ID
     * @param snapname snapshot name
     * @param vmType VM type (qemu or lxc)
     * @return snapshot deletion status
     */
    public String deleteSnapshot(String node, String vmid, String snapname, String vmType) {
        try {
            JsonNode result = "lxc".equalsIgnoreCase(vmType)
                ? responseData(proxmox.delete("/nodes/" + node + "/lxc/" + vmid + "/snapshot/" + snapname))
                : responseData(proxmox.delete("/nodes/" + node + "/qemu/" + vmid + "/snapshot/" + snapname));

            StringBuilder builder = new StringBuilder("Snapshot Deleted\n\n");
            builder.append("  Name: ").append(snapname).append("\n");
            builder.append("  ").append(vmType.toUpperCase()).append(" ID: ").append(vmid).append("\n");
            builder.append("  Node: ").append(node).append("\n\n");
            builder.append("Task ID: ").append(result);
            return builder.toString();
        } catch (Exception e) {
            return errorPayload("delete snapshot '" + snapname + "' for " + vmType + " " + vmid, e);
        }
    }

    /**
     * Rolls back a VM or container to a snapshot.
     *
     * @param node target node name
     * @param vmid VM or container ID
     * @param snapname snapshot name
     * @param vmType VM type (qemu or lxc)
     * @return rollback status message
     */
    public String rollbackSnapshot(String node, String vmid, String snapname, String vmType) {
        try {
            JsonNode snapshots = "lxc".equalsIgnoreCase(vmType)
                ? responseData(proxmox.get("/nodes/" + node + "/lxc/" + vmid + "/snapshot"))
                : responseData(proxmox.get("/nodes/" + node + "/qemu/" + vmid + "/snapshot"));

            List<String> deleted = new ArrayList<>();
            if (snapshots.isArray()) {
                for (JsonNode snap : snapshots) {
                    String parent = snap.path("parent").asText("");
                    String name = snap.path("name").asText("");
                    if (!"current".equals(name) && snapname.equals(parent)) {
                        try {
                            if ("lxc".equalsIgnoreCase(vmType)) {
                                proxmox.delete("/nodes/" + node + "/lxc/" + vmid + "/snapshot/" + name);
                            } else {
                                proxmox.delete("/nodes/" + node + "/qemu/" + vmid + "/snapshot/" + name);
                            }
                            deleted.add(name);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            JsonNode result = "lxc".equalsIgnoreCase(vmType)
                ? responseData(proxmox.postForm("/nodes/" + node + "/lxc/" + vmid + "/snapshot/" + snapname
                    + "/rollback", Map.of()))
                : responseData(proxmox.postForm("/nodes/" + node + "/qemu/" + vmid + "/snapshot/" + snapname
                    + "/rollback", Map.of()));

            StringBuilder builder = new StringBuilder("Snapshot Rollback Initiated\n\n");
            builder.append("  Restoring to: ").append(snapname).append("\n");
            builder.append("  ").append(vmType.toUpperCase()).append(" ID: ").append(vmid).append("\n");
            builder.append("  Node: ").append(node).append("\n");
            if (!deleted.isEmpty()) {
                builder.append("  Deleted newer snapshots: ").append(String.join(", ", deleted)).append("\n");
            }
            builder.append("\nWARNING: VM/container will be stopped during rollback!\n\n");
            builder.append("Task ID: ").append(result).append("\n\n");
            builder.append("The VM/container will be restored to its state at the time of the snapshot.");
            return builder.toString();
        } catch (Exception e) {
            return errorPayload("rollback to snapshot '" + snapname + "' for " + vmType + " " + vmid, e);
        }
    }

    /**
     * Builds an error payload for MCP responses.
     *
     * @param action the action being attempted
     * @param e the exception thrown
     * @return JSON error payload
     */
    private String errorPayload(String action, Exception e) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsString(Map.of("error", e.getMessage(), "action", action));
        } catch (Exception ignored) {
            return "{\"error\":\"" + e.getMessage() + "\",\"action\":\"" + action + "\"}";
        }
    }
}
