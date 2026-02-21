package io.github.smling.proxmoxmcpserver.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ConfigLoaderTests {

    @TempDir
    Path tempDir;

    @ParameterizedTest
    @NullAndEmptySource
    void loadConfigRejectsMissingPath(String configPath) {
        assertThatThrownBy(() -> ConfigLoader.loadConfig(configPath))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("PROXMOX_MCP_CONFIG");
    }

    @Test
    void loadConfigReadsValidFile() throws Exception {
        Path configPath = tempDir.resolve("config.json");
        Files.writeString(configPath, """
            {
              "proxmox": {
                "host": "pve.local",
                "port": 9000,
                "verify_ssl": false,
                "service": "PVE"
              },
              "auth": {
                "user": "root@pam",
                "token_name": "token",
                "token_value": "secret"
              },
              "mcp": {
                "host": "127.0.0.1",
                "port": 8000,
                "transport": "sse"
              }
            }
            """);

        Config config = ConfigLoader.loadConfig(configPath.toString());

        assertThat(config.getProxmox().getHost()).isEqualTo("pve.local");
        assertThat(config.getProxmox().getPort()).isEqualTo(9000);
        assertThat(config.getProxmox().isVerifySsl()).isFalse();
        assertThat(config.getAuth().getUser()).isEqualTo("root@pam");
        assertThat(config.getMcp().getTransport()).isEqualTo(McpTransport.SSE);
    }

    @Test
    void loadConfigRejectsMissingHost() throws Exception {
        Path configPath = tempDir.resolve("missing-host.json");
        Files.writeString(configPath, """
            {
              "proxmox": {
                "port": 8006
              }
            }
            """);

        assertThatThrownBy(() -> ConfigLoader.loadConfig(configPath.toString()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Proxmox host");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void loadConfigRejectsBlankHost(String host) throws Exception {
        Path configPath = tempDir.resolve("blank-host.json");
        Files.writeString(configPath, """
            {
              "proxmox": {
                "host": "%s"
              }
            }
            """.formatted(host));

        assertThatThrownBy(() -> ConfigLoader.loadConfig(configPath.toString()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Proxmox host");
    }

    @Test
    void loadConfigRejectsInvalidJson() throws Exception {
        Path configPath = tempDir.resolve("invalid.json");
        Files.writeString(configPath, "{ \"proxmox\": ");

        assertThatThrownBy(() -> ConfigLoader.loadConfig(configPath.toString()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid JSON");
    }

    @Test
    void loadConfigRejectsMissingFile() {
        Path configPath = tempDir.resolve("missing.json");

        assertThatThrownBy(() -> ConfigLoader.loadConfig(configPath.toString()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Failed to load config");
    }
}
