package io.github.smling.proxmoxmcpserver.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.smling.proxmoxmcpserver.core.ProxmoxClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class IsoTools extends ProxmoxTool {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public IsoTools(ProxmoxClient proxmox) {
        super(proxmox);
    }

    public String listIsos(String node, String storage) {
        try {
            List<JsonNode> isos = getStorageContent("iso", node, storage);
            if (isos.isEmpty()) {
                StringBuilder msg = new StringBuilder("No ISO images found");
                if (node != null && !node.isBlank()) {
                    msg.append(" on node ").append(node);
                }
                if (storage != null && !storage.isBlank()) {
                    msg.append(" in storage ").append(storage);
                }
                return msg.toString();
            }

            StringBuilder builder = new StringBuilder("Available ISO Images\n");
            isos.stream()
                .sorted(Comparator.comparing(n -> n.path("volid").asText("")))
                .forEach(iso -> appendContent(builder, iso));
            return builder.toString().trim();
        } catch (Exception e) {
            return errorPayload("list ISOs", e);
        }
    }

    public String listTemplates(String node, String storage) {
        try {
            List<JsonNode> templates = getStorageContent("vztmpl", node, storage);
            if (templates.isEmpty()) {
                StringBuilder msg = new StringBuilder("No OS templates found");
                if (node != null && !node.isBlank()) {
                    msg.append(" on node ").append(node);
                }
                if (storage != null && !storage.isBlank()) {
                    msg.append(" in storage ").append(storage);
                }
                return msg.toString();
            }

            StringBuilder builder = new StringBuilder("Available OS Templates\n");
            templates.stream()
                .sorted(Comparator.comparing(n -> n.path("volid").asText("")))
                .forEach(tmpl -> appendContent(builder, tmpl));
            builder.append("\nUse the Volume ID with createContainer's ostemplate parameter.");
            return builder.toString().trim();
        } catch (Exception e) {
            return errorPayload("list templates", e);
        }
    }

    public String downloadIso(
        String node,
        String storage,
        String url,
        String filename,
        String checksum,
        String checksumAlgorithm
    ) {
        try {
            Map<String, String> params = new java.util.HashMap<>();
            params.put("url", url);
            params.put("filename", filename);
            params.put("content", "iso");
            if (checksum != null && !checksum.isBlank()) {
                params.put("checksum", checksum);
                params.put("checksum-algorithm", checksumAlgorithm == null ? "sha256" : checksumAlgorithm);
            }

            JsonNode result = responseData(
                proxmox.postForm("/nodes/" + node + "/storage/" + storage + "/download-url", params)
            );
            StringBuilder builder = new StringBuilder("ISO Download Started\n\n");
            builder.append("  Filename: ").append(filename).append("\n");
            builder.append("  URL: ").append(url).append("\n");
            builder.append("  Storage: ").append(storage).append(" @ ").append(node).append("\n");
            if (checksum != null && !checksum.isBlank()) {
                builder.append("  Checksum: ").append((checksumAlgorithm == null ? "sha256" : checksumAlgorithm)
                    .toUpperCase(Locale.ROOT)).append("\n");
            }
            builder.append("\nTask ID: ").append(result).append("\n\n");
            builder.append("The download is running in the background.\nUse listIsos to verify when complete.");
            return builder.toString();
        } catch (Exception e) {
            return errorPayload("download ISO '" + filename + "'", e);
        }
    }

    public String deleteIso(String node, String storage, String filename) {
        try {
            String volid = filename;
            if (!filename.contains(":")) {
                JsonNode content = responseData(
                    proxmox.get("/nodes/" + node + "/storage/" + storage + "/content")
                );
                volid = null;
                for (JsonNode item : content) {
                    String itemVolid = item.path("volid").asText("");
                    if (itemVolid.contains(filename)) {
                        volid = itemVolid;
                        break;
                    }
                }
                if (volid == null) {
                    return "Error: Could not find '" + filename + "' in " + storage + " on " + node;
                }
            }

            JsonNode result = responseData(
                proxmox.delete("/nodes/" + node + "/storage/" + storage + "/content/" + volid)
            );
            StringBuilder builder = new StringBuilder("ISO/Template Deleted\n\n");
            builder.append("  Volume: ").append(volid).append("\n");
            builder.append("  Storage: ").append(storage).append("\n");
            builder.append("  Node: ").append(node).append("\n");
            if (result != null) {
                builder.append("\nTask ID: ").append(result);
            }
            return builder.toString();
        } catch (Exception e) {
            return errorPayload("delete ISO/template '" + filename + "'", e);
        }
    }

    private List<JsonNode> getStorageContent(String contentType, String node, String storage) {
        List<JsonNode> results = new ArrayList<>();
        JsonNode nodes;
        try {
            nodes = responseData(proxmox.get("/nodes"));
        } catch (Exception e) {
            handleError("list nodes", e);
            return results;
        }

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
                logger.warn("Skipping node {} while listing storage content", nodeName, e);
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
                if (!contentTypes.contains(contentType)) {
                    continue;
                }
                try {
                    JsonNode content = responseData(proxmox.get(
                        "/nodes/" + nodeName + "/storage/" + storageName + "/content",
                        Map.of("content", contentType)
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
        return results;
    }

    private void appendContent(StringBuilder builder, JsonNode item) {
        String volid = item.path("volid").asText("unknown");
        long size = item.path("size").asLong(0L);
        String node = item.path("_node").asText("?");
        String storage = item.path("_storage").asText("?");
        String filename = volid.contains("/") ? volid.substring(volid.lastIndexOf("/") + 1) : volid;
        builder.append("\n  ").append(filename).append("\n");
        builder.append("     Size: ").append(bytesToHuman(size)).append("\n");
        builder.append("     Storage: ").append(storage).append(" @ ").append(node).append("\n");
        builder.append("     Volume ID: ").append(volid).append("\n");
    }

    private String bytesToHuman(double n) {
        String[] units = {"B", "KiB", "MiB", "GiB", "TiB"};
        int i = 0;
        while (n >= 1024.0 && i < units.length - 1) {
            n /= 1024.0;
            i++;
        }
        return String.format(Locale.US, "%.2f %s", n, units[i]);
    }

    private String errorPayload(String action, Exception e) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsString(Map.of("error", e.getMessage(), "action", action));
        } catch (Exception ignored) {
            return "{\"error\":\"" + e.getMessage() + "\",\"action\":\"" + action + "\"}";
        }
    }
}
