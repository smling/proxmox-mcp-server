package io.github.smling.proxmoxmcpserver.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.smling.proxmoxmcpserver.core.ProxmoxClient;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Backup-related Proxmox operations.
 */
public class BackupTools extends ProxmoxTool {
    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    /**
     * Creates backup tools with a Proxmox client.
     *
     * @param proxmox the Proxmox client
     */
    public BackupTools(ProxmoxClient proxmox) {
        super(proxmox);
    }

    /**
     * Lists backups across matching nodes and storage pools.
     *
     * @param node optional node filter
     * @param storage optional storage filter
     * @param vmid optional VM or container filter
     * @return formatted backup list
     */
    public String listBackups(String node, String storage, String vmid) {
        try {
            List<JsonNode> results = new ArrayList<>();
            JsonNode nodes = responseData(proxmox.get("/nodes"));
            for (JsonNode n : nodes) {
                String nodeName = n.path("node").asText(null);
                if (nodeName == null) {
                    continue;
                }
                if (node != null && !node.isBlank() && !nodeName.equals(node)) {
                    continue;
                }

                JsonNode storages;
                try {
                    storages = responseData(proxmox.get("/nodes/" + nodeName + "/storage"));
                } catch (Exception e) {
                    logger.warn("Skipping node {} while listing backups", nodeName, e);
                    continue;
                }

                for (JsonNode store : storages) {
                    String storageName = store.path("storage").asText(null);
                    if (storageName == null) {
                        continue;
                    }
                    if (storage != null && !storage.isBlank() && !storageName.equals(storage)) {
                        continue;
                    }
                    String contentTypes = store.path("content").asText("");
                    if (!contentTypes.contains("backup")) {
                        continue;
                    }

                    try {
                        Map<String, String> params = new java.util.HashMap<>();
                        params.put("content", "backup");
                        if (vmid != null && !vmid.isBlank()) {
                            params.put("vmid", vmid);
                        }
                        JsonNode content = responseData(proxmox.get(
                            "/nodes/" + nodeName + "/storage/" + storageName + "/content",
                            params
                        ));
                        for (JsonNode item : content) {
                            ((com.fasterxml.jackson.databind.node.ObjectNode) item).put("_node", nodeName);
                            ((com.fasterxml.jackson.databind.node.ObjectNode) item).put("_storage", storageName);
                            results.add(item);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            if (results.isEmpty()) {
                StringBuilder msg = new StringBuilder("No backups found");
                if (node != null && !node.isBlank()) {
                    msg.append(" on node ").append(node);
                }
                if (storage != null && !storage.isBlank()) {
                    msg.append(" in storage ").append(storage);
                }
                if (vmid != null && !vmid.isBlank()) {
                    msg.append(" for VM/CT ").append(vmid);
                }
                return msg.toString();
            }

            results.sort(Comparator.<JsonNode>comparingLong(r -> r.path("ctime").asLong(0L)).reversed());

            StringBuilder builder = new StringBuilder("Available Backups\n");
            for (JsonNode backup : results) {
                String volid = backup.path("volid").asText("unknown");
                long size = backup.path("size").asLong(0L);
                long ctime = backup.path("ctime").asLong(0L);
                String backupVmid = backup.path("vmid").asText("?");
                String notes = backup.path("notes").asText("");
                boolean protectedFlag = backup.path("protected").asBoolean(false);
                String nodeName = backup.path("_node").asText("?");
                String storageName = backup.path("_storage").asText("?");
                String format = backup.path("format").asText("");

                String timeStr = ctime > 0 ? TIME_FORMAT.format(Instant.ofEpochSecond(ctime)) : "Unknown";

                builder.append("\n  VM/CT ").append(backupVmid).append(" - ").append(timeStr).append("\n");
                builder.append("     Size: ").append(bytesToHuman(size)).append("\n");
                builder.append("     Format: ").append(format).append("\n");
                builder.append("     Storage: ").append(storageName).append(" @ ").append(nodeName).append("\n");
                builder.append("     Volume ID: ").append(volid).append("\n");
                if (!notes.isBlank()) {
                    builder.append("     Notes: ").append(notes).append("\n");
                }
                if (protectedFlag) {
                    builder.append("     Protected\n");
                }
            }

            builder.append("\nUse the Volume ID with restoreBackup to restore.");
            return builder.toString().trim();
        } catch (Exception e) {
            return errorPayload("list backups", e);
        }
    }

    /**
     * Starts a backup task for a VM or container.
     *
     * @param node target node name
     * @param vmid VM or container ID
     * @param storage target storage pool
     * @param compress compression mode
     * @param mode backup mode
     * @param notes optional notes template
     * @return backup start status
     */
    public String createBackup(
        String node,
        String vmid,
        String storage,
        String compress,
        String mode,
        String notes
    ) {
        try {
            Map<String, String> params = new java.util.HashMap<>();
            params.put("vmid", vmid);
            params.put("storage", storage);
            params.put("compress", compress == null ? "zstd" : compress);
            params.put("mode", mode == null ? "snapshot" : mode);
            if (notes != null && !notes.isBlank()) {
                params.put("notes-template", notes);
            }

            JsonNode result = responseData(proxmox.postForm("/nodes/" + node + "/vzdump", params));
            StringBuilder builder = new StringBuilder("Backup Started\n\n");
            builder.append("  VM/CT ID: ").append(vmid).append("\n");
            builder.append("  Node: ").append(node).append("\n");
            builder.append("  Storage: ").append(storage).append("\n");
            builder.append("  Compression: ").append(compress == null ? "zstd" : compress).append("\n");
            builder.append("  Mode: ").append(mode == null ? "snapshot" : mode).append("\n");
            if (notes != null && !notes.isBlank()) {
                builder.append("  Notes: ").append(notes).append("\n");
            }
            builder.append("\nTask ID: ").append(taskId(result)).append("\n\n");
            builder.append("The backup is running in the background.\nUse listBackups to verify when complete.");
            return builder.toString();
        } catch (Exception e) {
            return errorPayload("create backup for " + vmid, e);
        }
    }

    /**
     * Restores a VM or container from a backup archive.
     *
     * @param node target node name
     * @param archive backup archive volume ID
     * @param vmid new VM or container ID
     * @param storage target storage pool
     * @param unique whether to generate unique MACs
     * @return restore status message
     */
    public String restoreBackup(
        String node,
        String archive,
        String vmid,
        String storage,
        boolean unique
    ) {
        try {
            boolean isLxc = archive.toLowerCase(Locale.ROOT).contains("/ct/")
                || archive.toLowerCase(Locale.ROOT).contains("vzdump-lxc");
            Map<String, String> params = new java.util.HashMap<>();
            params.put("archive", archive);
            params.put("vmid", vmid);
            if (storage != null && !storage.isBlank()) {
                params.put("storage", storage);
            }
            if (unique) {
                params.put("unique", "1");
            }

            JsonNode result = isLxc
                ? responseData(proxmox.postForm("/nodes/" + node + "/lxc", params))
                : responseData(proxmox.postForm("/nodes/" + node + "/qemu", params));
            String vmType = isLxc ? "Container" : "VM";

            StringBuilder builder = new StringBuilder(vmType + " Restore Started\n\n");
            builder.append("  New ID: ").append(vmid).append("\n");
            builder.append("  From: ").append(archive).append("\n");
            builder.append("  Target Node: ").append(node).append("\n");
            if (storage != null && !storage.isBlank()) {
                builder.append("  Target Storage: ").append(storage).append("\n");
            }
            builder.append("  Unique MACs: ").append(unique ? "Yes" : "No").append("\n");
            builder.append("\nTask ID: ").append(taskId(result)).append("\n\n");
            builder.append("The restore is running in the background.\nThe ")
                .append(vmType.toLowerCase(Locale.ROOT))
                .append(" will be available once the task completes.");
            return builder.toString();
        } catch (Exception e) {
            return errorPayload("restore backup to " + vmid, e);
        }
    }

    /**
     * Deletes a backup volume from storage.
     *
     * @param node target node name
     * @param storage target storage pool
     * @param volid backup volume ID
     * @return deletion status message
     */
    public String deleteBackup(String node, String storage, String volid) {
        try {
            JsonNode content = responseData(proxmox.get(
                "/nodes/" + node + "/storage/" + storage + "/content",
                Map.of("content", "backup")
            ));

            JsonNode backupInfo = null;
            for (JsonNode item : content) {
                if (volid.equals(item.path("volid").asText())) {
                    backupInfo = item;
                    break;
                }
            }

            if (backupInfo != null && backupInfo.path("protected").asBoolean(false)) {
                return "Error: Backup '" + volid + "' is protected and cannot be deleted.\n"
                    + "Remove protection first if you want to delete it.";
            }

            JsonNode result = responseData(
                proxmox.delete("/nodes/" + node + "/storage/" + storage + "/content/" + volid)
            );
            StringBuilder builder = new StringBuilder("Backup Deleted\n\n");
            builder.append("  Volume: ").append(volid).append("\n");
            builder.append("  Storage: ").append(storage).append("\n");
            builder.append("  Node: ").append(node).append("\n");
            if (result != null) {
                builder.append("\nTask ID: ").append(taskId(result));
            }
            return builder.toString();
        } catch (Exception e) {
            return errorPayload("delete backup '" + volid + "'", e);
        }
    }

    /**
     * Formats bytes using IEC units.
     *
     * @param n the byte count
     * @return formatted size string
     */
    private String bytesToHuman(double n) {
        String[] units = {"B", "KiB", "MiB", "GiB", "TiB"};
        int i = 0;
        while (n >= 1024.0 && i < units.length - 1) {
            n /= 1024.0;
            i++;
        }
        return String.format(Locale.US, "%.2f %s", n, units[i]);
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

    static ObjectMapper swapObjectMapper(ObjectMapper replacement) {
        ObjectMapper original = OBJECT_MAPPER;
        OBJECT_MAPPER = replacement;
        return original;
    }
}
