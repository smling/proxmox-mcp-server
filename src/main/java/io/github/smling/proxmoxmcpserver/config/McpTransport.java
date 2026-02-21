package io.github.smling.proxmoxmcpserver.config;

public enum McpTransport {
    STDIO,
    SSE,
    STREAMABLE;

    public static McpTransport fromString(String value) {
        if (value == null || value.isBlank()) {
            return STDIO;
        }
        String normalized = value.trim().toUpperCase();
        if ("STREAMABLE_HTTP".equals(normalized)) {
            normalized = "STREAMABLE";
        }
        return McpTransport.valueOf(normalized);
    }
}
