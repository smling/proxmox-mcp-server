package io.github.smling.proxmoxmcpserver.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.smling.proxmoxmcpserver.core.ProxmoxClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ContainerTools extends ProxmoxTool {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public ContainerTools(ProxmoxClient proxmox) {
        super(proxmox);
    }

    public String getContainers(String node, boolean includeStats, boolean includeRaw, String formatStyle) {
        try {
            List<NodeContainerPair> pairs = listCtPairs(node);
            List<Map<String, Object>> rows = new ArrayList<>();

            for (NodeContainerPair pair : pairs) {
                JsonNode ct = pair.container();
                String vmidValue = ct.path("vmid").asText(null);
                Integer vmidInt = null;
                try {
                    if (vmidValue != null) {
                        vmidInt = Integer.parseInt(vmidValue);
                    }
                } catch (NumberFormatException ignored) {
                    vmidInt = null;
                }

                Map<String, Object> rec = new HashMap<>();
                rec.put("vmid", vmidValue);
                rec.put("name", firstText(ct, "name", "hostname",
                    vmidValue == null ? "ct-?" : "ct-" + vmidValue));
                rec.put("node", pair.node());
                rec.put("status", ct.path("status").asText(null));

                if (includeStats && vmidInt != null) {
                    JsonNode status = getOrEmpty("/nodes/" + pair.node() + "/lxc/" + vmidInt + "/status/current");
                    JsonNode config = getOrEmpty("/nodes/" + pair.node() + "/lxc/" + vmidInt + "/config");

                    double cpuFrac = status.path("cpu").asDouble(0.0);
                    double cpuPct = roundTwo(cpuFrac * 100.0);
                    long memBytes = status.path("mem").asLong(0L);
                    long maxmemBytes = status.path("maxmem").asLong(0L);

                    int memoryMib = intFromAny(config, "memory", "ram", "maxmem", "memoryMiB");
                    boolean unlimitedMemory = config.path("swap").asInt(0) == 0 && memoryMib == 0;

                    Double cores = null;
                    if (config.has("cores")) {
                        cores = config.path("cores").asDouble();
                    } else if (config.has("cpulimit")) {
                        double cpulimit = config.path("cpulimit").asDouble();
                        if (cpulimit > 0) {
                            cores = cpulimit;
                        }
                    }

                    String statusStr = status.path("status").asText(ct.path("status").asText("")).toLowerCase(Locale.ROOT);
                    if ("stopped".equals(statusStr)) {
                        memBytes = 0L;
                    }

                    if (maxmemBytes == 0 && memoryMib > 0) {
                        maxmemBytes = memoryMib * 1024L * 1024L;
                    }

                    if (memBytes == 0 || maxmemBytes == 0 || cpuPct == 0.0) {
                        RrdSample rrd = rrdLast(pair.node(), vmidInt);
                        if (cpuPct == 0.0 && rrd.cpuPct() != null) {
                            cpuPct = rrd.cpuPct();
                        }
                        if (memBytes == 0 && rrd.memBytes() != null) {
                            memBytes = rrd.memBytes();
                        }
                        if (maxmemBytes == 0 && rrd.maxmemBytes() != null) {
                            maxmemBytes = rrd.maxmemBytes();
                            if (memoryMib == 0) {
                                memoryMib = (int) Math.round(maxmemBytes / (1024.0 * 1024.0));
                            }
                        }
                    }

                    rec.put("cores", cores);
                    rec.put("memory", memoryMib);
                    rec.put("cpu_pct", cpuPct);
                    rec.put("mem_bytes", memBytes);
                    rec.put("maxmem_bytes", maxmemBytes);
                    rec.put("mem_pct", maxmemBytes > 0 ? roundTwo((double) memBytes / maxmemBytes * 100.0) : null);
                    rec.put("unlimited_memory", unlimitedMemory);

                    if (includeRaw && !"json".equalsIgnoreCase(formatStyle)) {
                        rec.put("raw_status", status);
                        rec.put("raw_config", config);
                    }
                }

                rows.add(rec);
            }

            if ("json".equalsIgnoreCase(formatStyle)) {
                return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(rows);
            }
            return renderPretty(rows);
        } catch (Exception e) {
            return errorPayload("Failed to list containers", e);
        }
    }

    public String startContainer(String selector, String formatStyle) {
        return containerAction("Start Containers", selector, formatStyle, (node, vmid) ->
            responseData(proxmox.postForm("/nodes/" + node + "/lxc/" + vmid + "/status/start", Map.of()))
        );
    }

    public String stopContainer(String selector, boolean graceful, int timeoutSeconds, String formatStyle) {
        return containerAction("Stop Containers", selector, formatStyle, (node, vmid) -> {
            if (graceful) {
                return responseData(proxmox.postForm(
                    "/nodes/" + node + "/lxc/" + vmid + "/status/shutdown",
                    Map.of("timeout", String.valueOf(timeoutSeconds))
                ));
            }
            return responseData(
                proxmox.postForm("/nodes/" + node + "/lxc/" + vmid + "/status/stop", Map.of())
            );
        });
    }

    public String restartContainer(String selector, int timeoutSeconds, String formatStyle) {
        return containerAction("Restart Containers", selector, formatStyle, (node, vmid) ->
            responseData(proxmox.postForm("/nodes/" + node + "/lxc/" + vmid + "/status/reboot", Map.of()))
        );
    }

    public String createContainer(
        String node,
        String vmid,
        String ostemplate,
        String hostname,
        int cores,
        int memory,
        int swap,
        int diskSize,
        String storage,
        String password,
        String sshPublicKeys,
        String networkBridge,
        boolean startAfterCreate,
        boolean unprivileged
    ) {
        try {
            for (NodeContainerPair pair : listCtPairs(null)) {
                if (vmid.equals(pair.container().path("vmid").asText())) {
                    return errorPayload("Container with ID " + vmid + " already exists on node " + pair.node(),
                        new IllegalArgumentException("VMID " + vmid + " already in use"));
                }
            }

            List<String> nodeNames = new ArrayList<>();
            JsonNode nodes = responseData(proxmox.get("/nodes"));
            for (JsonNode entry : nodes) {
                nodeNames.add(entry.path("node").asText());
            }
            if (!nodeNames.contains(node)) {
                return errorPayload("Node '" + node + "' not found",
                    new IllegalArgumentException("Available nodes: " + String.join(", ", nodeNames)));
            }

            if (storage == null || storage.isBlank()) {
                JsonNode storageList = responseData(proxmox.get("/storage"));
                for (JsonNode store : storageList) {
                    String name = store.path("storage").asText();
                    String content = store.path("content").asText("");
                    if ("local-lvm".equals(name)) {
                        storage = name;
                        break;
                    }
                    if (content.contains("rootdir") || content.contains("images")) {
                        storage = name;
                    }
                }
                if (storage == null || storage.isBlank()) {
                    storage = !storageList.isEmpty() ? storageList.get(0).path("storage").asText("local") : "local";
                }
            }

            if (hostname == null || hostname.isBlank()) {
                hostname = "ct-" + vmid;
            }
            if (networkBridge == null || networkBridge.isBlank()) {
                networkBridge = "vmbr0";
            }

            Map<String, String> config = new HashMap<>();
            config.put("vmid", vmid);
            config.put("ostemplate", ostemplate);
            config.put("hostname", hostname);
            config.put("cores", String.valueOf(cores));
            config.put("memory", String.valueOf(memory));
            config.put("swap", String.valueOf(swap));
            config.put("rootfs", storage + ":" + diskSize);
            config.put("net0", "name=eth0,bridge=" + networkBridge + ",ip=dhcp");
            config.put("unprivileged", unprivileged ? "1" : "0");
            config.put("start", startAfterCreate ? "1" : "0");
            if (password != null && !password.isBlank()) {
                config.put("password", password);
            }
            if (sshPublicKeys != null && !sshPublicKeys.isBlank()) {
                config.put("ssh-public-keys", sshPublicKeys);
            }

            JsonNode result = responseData(proxmox.postForm("/nodes/" + node + "/lxc", config));
            StringBuilder builder = new StringBuilder();
            builder.append("Container Created Successfully\n\n");
            builder.append("  VMID: ").append(vmid).append("\n");
            builder.append("  Hostname: ").append(hostname).append("\n");
            builder.append("  Node: ").append(node).append("\n");
            builder.append("  Template: ").append(ostemplate).append("\n");
            builder.append("  CPU Cores: ").append(cores).append("\n");
            builder.append("  Memory: ").append(memory).append(" MiB\n");
            builder.append("  Swap: ").append(swap).append(" MiB\n");
            builder.append("  Disk: ").append(diskSize).append(" GB on ").append(storage).append("\n");
            builder.append("  Network: ").append(networkBridge).append(" (DHCP)\n");
            builder.append("  Unprivileged: ").append(unprivileged ? "Yes" : "No").append("\n");
            builder.append("  Auto-start: ").append(startAfterCreate ? "Yes" : "No").append("\n\n");
            builder.append("Task ID: ").append(result).append("\n\n");
            builder.append("Next steps:\n");
            builder.append("  - Start container: startContainer selector='").append(vmid).append("'\n");
            builder.append("  - Check status: getContainers");
            return builder.toString();
        } catch (Exception e) {
            return errorPayload("Failed to create container " + vmid, e);
        }
    }

    public String deleteContainer(String selector, boolean force, String formatStyle) {
        try {
            List<ContainerTarget> targets = resolveTargets(selector);
            if (targets.isEmpty()) {
                return errorPayload("No containers matched the selector", new IllegalArgumentException(selector));
            }

            List<Map<String, Object>> results = new ArrayList<>();
            for (ContainerTarget target : targets) {
                Map<String, Object> rec = new HashMap<>();
                rec.put("ok", true);
                rec.put("node", target.node());
                rec.put("vmid", target.vmid());
                rec.put("name", target.label());
                try {
                    JsonNode status = responseData(proxmox.get("/nodes/" + target.node() + "/lxc/" + target.vmid()
                        + "/status/current"));
                    String currentStatus = status.path("status").asText("").toLowerCase(Locale.ROOT);
                    if ("running".equals(currentStatus)) {
                        if (!force) {
                            rec.put("ok", false);
                            rec.put("error", "Container is running. Use force=true to stop and delete.");
                            results.add(rec);
                            continue;
                        }
                        proxmox.postForm("/nodes/" + target.node() + "/lxc/" + target.vmid() + "/status/stop", Map.of());
                        rec.put("message", "Stopped and deleted");
                    } else {
                        rec.put("message", "Deleted");
                    }

                    JsonNode task = responseData(
                        proxmox.delete("/nodes/" + target.node() + "/lxc/" + target.vmid())
                    );
                    rec.put("task_id", String.valueOf(task));
                } catch (Exception e) {
                    rec.put("ok", false);
                    rec.put("error", e.getMessage());
                }
                results.add(rec);
            }

            if ("json".equalsIgnoreCase(formatStyle)) {
                return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(results);
            }
            return renderActionResult("Delete Containers", results);
        } catch (Exception e) {
            return errorPayload("Failed to delete container(s)", e);
        }
    }

    public String updateContainerResources(
        String selector,
        Integer cores,
        Integer memory,
        Integer swap,
        Integer diskGb,
        String disk,
        String formatStyle
    ) {
        try {
            List<ContainerTarget> targets = resolveTargets(selector);
            if (targets.isEmpty()) {
                return errorPayload("No containers matched the selector", new IllegalArgumentException(selector));
            }

            List<Map<String, Object>> results = new ArrayList<>();
            for (ContainerTarget target : targets) {
                Map<String, Object> rec = new HashMap<>();
                rec.put("ok", true);
                rec.put("node", target.node());
                rec.put("vmid", target.vmid());
                rec.put("name", target.label());
                List<String> changes = new ArrayList<>();

                try {
                    Map<String, String> update = new HashMap<>();
                    if (cores != null) {
                        update.put("cores", String.valueOf(cores));
                        changes.add("cores=" + cores);
                    }
                    if (memory != null) {
                        update.put("memory", String.valueOf(memory));
                        changes.add("memory=" + memory + "MiB");
                    }
                    if (swap != null) {
                        update.put("swap", String.valueOf(swap));
                        changes.add("swap=" + swap + "MiB");
                    }
                    if (!update.isEmpty()) {
                        proxmox.putForm("/nodes/" + target.node() + "/lxc/" + target.vmid() + "/config", update);
                    }

                    if (diskGb != null) {
                        String size = "+" + diskGb + "G";
                        proxmox.putForm("/nodes/" + target.node() + "/lxc/" + target.vmid() + "/resize",
                            Map.of("disk", disk == null ? "rootfs" : disk, "size", size));
                        changes.add((disk == null ? "rootfs" : disk) + "+=" + diskGb + "G");
                    }

                    rec.put("message", changes.isEmpty() ? "no changes" : String.join(", ", changes));
                } catch (Exception e) {
                    rec.put("ok", false);
                    rec.put("error", e.getMessage());
                }

                results.add(rec);
            }

            if ("json".equalsIgnoreCase(formatStyle)) {
                return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(results);
            }
            return renderActionResult("Update Container Resources", results);
        } catch (Exception e) {
            return errorPayload("Failed to update container(s)", e);
        }
    }

    private String containerAction(String title, String selector, String formatStyle, Action action) {
        try {
            List<ContainerTarget> targets = resolveTargets(selector);
            if (targets.isEmpty()) {
                return errorPayload("No containers matched the selector", new IllegalArgumentException(selector));
            }

            List<Map<String, Object>> results = new ArrayList<>();
            for (ContainerTarget target : targets) {
                Map<String, Object> rec = new HashMap<>();
                rec.put("ok", true);
                rec.put("node", target.node());
                rec.put("vmid", target.vmid());
                rec.put("name", target.label());
                try {
                    Object resp = action.apply(target.node(), target.vmid());
                    rec.put("message", resp);
                } catch (Exception e) {
                    rec.put("ok", false);
                    rec.put("error", e.getMessage());
                }
                results.add(rec);
            }

            if ("json".equalsIgnoreCase(formatStyle)) {
                return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(results);
            }
            return renderActionResult(title, results);
        } catch (Exception e) {
            return errorPayload("Failed to " + title.toLowerCase(Locale.ROOT), e);
        }
    }

    private List<NodeContainerPair> listCtPairs(String node) {
        List<NodeContainerPair> out = new ArrayList<>();
        if (node != null && !node.isBlank()) {
            try {
                JsonNode raw = responseData(proxmox.get("/nodes/" + node + "/lxc"));
                for (JsonNode item : raw) {
                    if (item.isObject()) {
                        out.add(new NodeContainerPair(node, item));
                    } else if (item.isNumber()) {
                        out.add(new NodeContainerPair(node, OBJECT_MAPPER.createObjectNode()
                            .put("vmid", item.asInt())));
                    }
                }
            } catch (Exception e) {
                logger.warn("Skipping node {} while listing containers", node, e);
            }
            return out;
        }

        JsonNode nodes;
        try {
            nodes = responseData(proxmox.get("/nodes"));
        } catch (Exception e) {
            handleError("list containers", e);
            return out;
        }

        for (JsonNode n : nodes) {
            String nodeName = n.path("node").asText(null);
            if (nodeName == null) {
                continue;
            }
            try {
                JsonNode raw = responseData(proxmox.get("/nodes/" + nodeName + "/lxc"));
                for (JsonNode item : raw) {
                    if (item.isObject()) {
                        out.add(new NodeContainerPair(nodeName, item));
                    } else if (item.isNumber()) {
                        out.add(new NodeContainerPair(nodeName, OBJECT_MAPPER.createObjectNode()
                            .put("vmid", item.asInt())));
                    }
                }
            } catch (Exception e) {
                logger.warn("Skipping node {} while listing containers", nodeName, e);
            }
        }
        return out;
    }

    private List<ContainerTarget> resolveTargets(String selector) {
        List<ContainerTarget> resolved = new ArrayList<>();
        if (selector == null || selector.isBlank()) {
            return resolved;
        }
        String[] tokens = selector.split(",");
        List<NodeContainerPair> inventory = listCtPairs(null);

        for (String rawToken : tokens) {
            String token = rawToken.trim();
            if (token.isEmpty()) {
                continue;
            }
            if (token.contains(":") && !token.contains("/")) {
                String[] parts = token.split(":", 2);
                String node = parts[0];
                int vmid = parseInt(parts[1]);
                if (vmid < 0) {
                    continue;
                }
                for (NodeContainerPair pair : inventory) {
                    if (pair.node().equals(node) && parseInt(pair.container().path("vmid").asText()) == vmid) {
                        String label = firstText(pair.container(), "name", "hostname", "ct-" + vmid);
                        resolved.add(new ContainerTarget(node, vmid, label));
                        break;
                    }
                }
                continue;
            }
            if (token.contains("/") && !token.contains(":")) {
                String[] parts = token.split("/", 2);
                String node = parts[0];
                String name = parts[1].trim();
                for (NodeContainerPair pair : inventory) {
                    String ctName = firstText(pair.container(), "name", "hostname", null);
                    if (pair.node().equals(node) && name.equals(ctName)) {
                        int vmid = parseInt(pair.container().path("vmid").asText());
                        if (vmid >= 0) {
                            resolved.add(new ContainerTarget(node, vmid, name));
                        }
                    }
                }
                continue;
            }
            if (token.chars().allMatch(Character::isDigit)) {
                int vmid = parseInt(token);
                for (NodeContainerPair pair : inventory) {
                    if (parseInt(pair.container().path("vmid").asText()) == vmid) {
                        String label = firstText(pair.container(), "name", "hostname", "ct-" + vmid);
                        resolved.add(new ContainerTarget(pair.node(), vmid, label));
                    }
                }
                continue;
            }

            String name = token;
            for (NodeContainerPair pair : inventory) {
                String ctName = firstText(pair.container(), "name", "hostname", null);
                if (name.equals(ctName)) {
                    int vmid = parseInt(pair.container().path("vmid").asText());
                    if (vmid >= 0) {
                        resolved.add(new ContainerTarget(pair.node(), vmid, name));
                    }
                }
            }
        }

        Map<String, ContainerTarget> uniq = new HashMap<>();
        for (ContainerTarget target : resolved) {
            String key = target.node() + ":" + target.vmid();
            uniq.put(key, target);
        }
        return new ArrayList<>(uniq.values());
    }

    private String renderPretty(List<Map<String, Object>> rows) {
        StringBuilder builder = new StringBuilder("Containers\n");
        for (Map<String, Object> row : rows) {
            String name = row.get("name") == null ? "ct-" + row.get("vmid") : row.get("name").toString();
            builder.append("\n").append(name).append(" (ID: ").append(row.get("vmid")).append(")\n");
            builder.append("  Status: ").append(stringValue(row.get("status")).toUpperCase(Locale.ROOT)).append("\n");
            builder.append("  Node: ").append(stringValue(row.get("node"))).append("\n");
            builder.append("  CPU: ").append(String.format(Locale.US, "%.1f%%", doubleValue(row.get("cpu_pct"))))
                .append("\n");
            builder.append("  CPU Cores: ").append(row.get("cores") == null ? "N/A" : row.get("cores")).append("\n");

            boolean unlimited = Boolean.TRUE.equals(row.get("unlimited_memory"));
            long memBytes = longValue(row.get("mem_bytes"));
            long maxmemBytes = longValue(row.get("maxmem_bytes"));
            if (unlimited) {
                builder.append("  Memory: ").append(bytesToHuman(memBytes)).append(" (unlimited)\n");
            } else if (maxmemBytes > 0) {
                String pctStr = row.get("mem_pct") instanceof Number
                    ? String.format(Locale.US, " (%.1f%%)", doubleValue(row.get("mem_pct")))
                    : "";
                builder.append("  Memory: ").append(bytesToHuman(memBytes)).append(" / ")
                    .append(bytesToHuman(maxmemBytes)).append(pctStr).append("\n");
            } else {
                builder.append("  Memory: ").append(bytesToHuman(memBytes)).append(" / 0.00 B\n");
            }
        }
        return builder.toString().trim();
    }

    private String renderActionResult(String title, List<Map<String, Object>> results) {
        StringBuilder builder = new StringBuilder(title).append("\n");
        for (Map<String, Object> result : results) {
            boolean ok = Boolean.TRUE.equals(result.get("ok"));
            String status = ok ? "OK" : "FAIL";
            String node = stringValue(result.get("node"));
            String vmid = stringValue(result.get("vmid"));
            String name = result.get("name") == null ? "ct-" + vmid : result.get("name").toString();
            String message = result.get("message") == null ? "" : " - " + result.get("message");
            String error = result.get("error") == null ? "" : " - " + result.get("error");
            builder.append(status).append(" ").append(name)
                .append(" (ID: ").append(vmid).append(", node: ").append(node).append(")")
                .append(ok ? message : error)
                .append("\n");
        }
        return builder.toString().trim();
    }

    private String errorPayload(String action, Exception e) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsString(Map.of("error", e.getMessage(), "action", action));
        } catch (Exception ignored) {
            return "{\"error\":\"" + e.getMessage() + "\",\"action\":\"" + action + "\"}";
        }
    }

    private JsonNode getOrEmpty(String path) {
        try {
            return responseData(proxmox.get(path));
        } catch (Exception e) {
            return OBJECT_MAPPER.createObjectNode();
        }
    }

    private String bytesToHuman(double n) {
        String[] units = {"B", "KiB", "MiB", "GiB", "TiB", "PiB"};
        int i = 0;
        while (n >= 1024.0 && i < units.length - 1) {
            n /= 1024.0;
            i++;
        }
        return String.format(Locale.US, "%.2f %s", n, units[i]);
    }

    private double roundTwo(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return -1;
        }
    }

    private int intFromAny(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key)) {
                try {
                    return node.path(key).asInt();
                } catch (Exception ignored) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private String firstText(JsonNode node, String key1, String key2, String fallback) {
        if (node.hasNonNull(key1)) {
            return node.path(key1).asText();
        }
        if (node.hasNonNull(key2)) {
            return node.path(key2).asText();
        }
        return fallback;
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private double doubleValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }

    private String stringValue(Object value) {
        return value == null ? "N/A" : value.toString();
    }

    private RrdSample rrdLast(String node, int vmid) {
        try {
            JsonNode rrd = responseData(proxmox.get("/nodes/" + node + "/lxc/" + vmid + "/rrddata",
                Map.of("timeframe", "hour", "ds", "cpu,mem,maxmem")));
            if (!rrd.isArray() || rrd.isEmpty()) {
                return new RrdSample(null, null, null);
            }
            JsonNode last = rrd.get(rrd.size() - 1);
            Double cpuPct = last.has("cpu") ? last.path("cpu").asDouble(0.0) * 100.0 : null;
            Long memBytes = last.has("mem") ? last.path("mem").asLong(0L) : null;
            Long maxmemBytes = last.has("maxmem") ? last.path("maxmem").asLong(0L) : null;
            return new RrdSample(cpuPct, memBytes, maxmemBytes);
        } catch (Exception e) {
            return new RrdSample(null, null, null);
        }
    }

    private record NodeContainerPair(String node, JsonNode container) {
    }

    private record ContainerTarget(String node, int vmid, String label) {
    }

    private record RrdSample(Double cpuPct, Long memBytes, Long maxmemBytes) {
    }

    private interface Action {
        Object apply(String node, int vmid) throws Exception;
    }
}
