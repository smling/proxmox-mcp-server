package io.github.smling.proxmoxmcpserver.config;

/**
 * Supported MCP transport types.
 */
public enum McpTransport {
    STDIO,
    SSE,
    STREAMABLE;

    /**
     * Parses a transport identifier into a {@link McpTransport} value.
     *
     * @param value the transport name
     * @return the matching transport, defaulting to {@link #STDIO}
     */
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
