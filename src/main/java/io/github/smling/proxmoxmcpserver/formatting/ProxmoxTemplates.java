package io.github.smling.proxmoxmcpserver.formatting;

import java.util.List;
import java.util.Map;

/**
 * Template helpers for rendering Proxmox API responses.
 */
public final class ProxmoxTemplates {
    /**
     * Prevents instantiation of this utility class.
     */
    private ProxmoxTemplates() {
    }

    /**
     * Renders a list of nodes as a human-readable summary.
     *
     * @param nodes the node records
     * @return formatted node list
     */
    public static String nodeList(List<Map<String, Object>> nodes) {
        StringBuilder builder = new StringBuilder("Proxmox Nodes");
        for (Map<String, Object> node : nodes) {
            builder.append("\n\nNode: ").append(node.get("node"));
            builder.append("\n  Status: ").append(stringValue(node.get("status")).toUpperCase());
            builder.append("\n  Uptime: ").append(ProxmoxFormatters.formatUptime(longValue(node.get("uptime"))));
            builder.append("\n  CPU Cores: ").append(stringValue(node.get("maxcpu")));
            Map<String, Object> memory = mapValue(node.get("memory"));
            long used = longValue(memory.get("used"));
            long total = longValue(memory.get("total"));
            double percent = total > 0 ? (double) used / total * 100 : 0;
            builder.append("\n  Memory: ")
                .append(ProxmoxFormatters.formatBytes(used))
                .append(" / ")
                .append(ProxmoxFormatters.formatBytes(total))
                .append(String.format(" (%.1f%%)", percent));
        }
        return builder.toString();
    }

    /**
     * Renders a single node status summary.
     *
     * @param node the node name
     * @param status the node status payload
     * @return formatted node status
     */
    public static String nodeStatus(String node, Map<String, Object> status) {
        StringBuilder builder = new StringBuilder("Node: ").append(node);
        builder.append("\n  Status: ").append(stringValue(status.get("status")).toUpperCase());
        builder.append("\n  Uptime: ").append(ProxmoxFormatters.formatUptime(longValue(status.get("uptime"))));
        builder.append("\n  CPU Cores: ").append(stringValue(status.get("maxcpu")));
        Map<String, Object> memory = mapValue(status.get("memory"));
        long used = longValue(memory.get("used"));
        long total = longValue(memory.get("total"));
        double percent = total > 0 ? (double) used / total * 100 : 0;
        builder.append("\n  Memory: ")
            .append(ProxmoxFormatters.formatBytes(used))
            .append(" / ")
            .append(ProxmoxFormatters.formatBytes(total))
            .append(String.format(" (%.1f%%)", percent));
        return builder.toString();
    }

    /**
     * Renders a list of VMs as a human-readable summary.
     *
     * @param vms the VM records
     * @return formatted VM list
     */
    public static String vmList(List<Map<String, Object>> vms) {
        StringBuilder builder = new StringBuilder("Virtual Machines");
        for (Map<String, Object> vm : vms) {
            builder.append("\n\nVM: ").append(vm.get("name")).append(" (ID: ").append(vm.get("vmid")).append(")");
            builder.append("\n  Status: ").append(stringValue(vm.get("status")).toUpperCase());
            builder.append("\n  Node: ").append(stringValue(vm.get("node")));
            builder.append("\n  CPU Cores: ").append(stringValue(vm.get("cpus")));
            Map<String, Object> memory = mapValue(vm.get("memory"));
            long used = longValue(memory.get("used"));
            long total = longValue(memory.get("total"));
            double percent = total > 0 ? (double) used / total * 100 : 0;
            builder.append("\n  Memory: ")
                .append(ProxmoxFormatters.formatBytes(used))
                .append(" / ")
                .append(ProxmoxFormatters.formatBytes(total))
                .append(String.format(" (%.1f%%)", percent));
        }
        return builder.toString();
    }

    /**
     * Renders a list of storage pools as a human-readable summary.
     *
     * @param storage the storage records
     * @return formatted storage list
     */
    public static String storageList(List<Map<String, Object>> storage) {
        StringBuilder builder = new StringBuilder("Storage Pools");
        for (Map<String, Object> store : storage) {
            long used = longValue(store.get("used"));
            long total = longValue(store.get("total"));
            double percent = total > 0 ? (double) used / total * 100 : 0;
            builder.append("\n\nStorage: ").append(store.get("storage"));
            builder.append("\n  Status: ").append(stringValue(store.get("status")).toUpperCase());
            builder.append("\n  Type: ").append(stringValue(store.get("type")));
            builder.append("\n  Usage: ")
                .append(ProxmoxFormatters.formatBytes(used))
                .append(" / ")
                .append(ProxmoxFormatters.formatBytes(total))
                .append(String.format(" (%.1f%%)", percent));
        }
        return builder.toString();
    }

    /**
     * Renders a list of containers as a human-readable summary.
     *
     * @param containers the container records
     * @return formatted container list
     */
    public static String containerList(List<Map<String, Object>> containers) {
        if (containers.isEmpty()) {
            return "No containers found";
        }
        StringBuilder builder = new StringBuilder("Containers");
        for (Map<String, Object> container : containers) {
            builder.append("\n\nContainer: ").append(container.get("name"))
                .append(" (ID: ").append(container.get("vmid")).append(")");
            builder.append("\n  Status: ").append(stringValue(container.get("status")).toUpperCase());
            builder.append("\n  Node: ").append(stringValue(container.get("node")));
            builder.append("\n  CPU Cores: ").append(stringValue(container.get("cpus")));
            Map<String, Object> memory = mapValue(container.get("memory"));
            long used = longValue(memory.get("used"));
            long total = longValue(memory.get("total"));
            double percent = total > 0 ? (double) used / total * 100 : 0;
            builder.append("\n  Memory: ")
                .append(ProxmoxFormatters.formatBytes(used))
                .append(" / ")
                .append(ProxmoxFormatters.formatBytes(total))
                .append(String.format(" (%.1f%%)", percent));
        }
        return builder.toString();
    }

    /**
     * Renders a cluster status summary.
     *
     * @param status the cluster status payload
     * @return formatted cluster status
     */
    public static String clusterStatus(Map<String, Object> status) {
        StringBuilder builder = new StringBuilder("Proxmox Cluster");
        builder.append("\n\n  Name: ").append(stringValue(status.get("name")));
        builder.append("\n  Quorum: ").append(booleanValue(status.get("quorum")) ? "OK" : "NOT OK");
        builder.append("\n  Nodes: ").append(longValue(status.get("nodes")));
        Object resources = status.get("resources");
        if (resources instanceof List<?> list) {
            builder.append("\n  Resources: ").append(list.size());
        }
        return builder.toString();
    }

    /**
     * Converts a value to a map when possible.
     *
     * @param value the value to inspect
     * @return the map value or an empty map
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    /**
     * Converts a value to a long, defaulting to 0.
     *
     * @param value the value to inspect
     * @return the long value or 0
     */
    private static long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0;
    }

    /**
     * Converts a value to a boolean, defaulting to false.
     *
     * @param value the value to inspect
     * @return the boolean value or false
     */
    private static boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return false;
    }

    /**
     * Converts a value to a string, defaulting to {@code N/A}.
     *
     * @param value the value to inspect
     * @return the string representation
     */
    private static String stringValue(Object value) {
        return value == null ? "N/A" : value.toString();
    }
}
