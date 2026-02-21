package io.github.smling.proxmoxmcpserver.formatting;

public final class ProxmoxFormatters {
    private ProxmoxFormatters() {
    }

    public static String formatBytes(long bytesValue) {
        double value = bytesValue;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int index = 0;
        while (value >= 1024 && index < units.length - 1) {
            value /= 1024;
            index++;
        }
        return String.format("%.2f %s", value, units[index]);
    }

    public static String formatUptime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("d ");
        }
        if (hours > 0) {
            builder.append(hours).append("h ");
        }
        if (minutes > 0) {
            builder.append(minutes).append("m");
        }
        String result = builder.toString().trim();
        return result.isEmpty() ? "0m" : result;
    }

    public static String formatCommandOutput(boolean success, String command, String output, String error) {
        StringBuilder builder = new StringBuilder("Console Command Result");
        builder.append("\n  Status: ").append(success ? "SUCCESS" : "FAILED");
        builder.append("\n  Command: ").append(command);
        builder.append("\n\nOutput:\n").append(output == null ? "" : output.trim());
        if (error != null && !error.isBlank()) {
            builder.append("\n\nError:\n").append(error.trim());
        }
        return builder.toString();
    }
}
