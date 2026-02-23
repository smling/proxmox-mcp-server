package io.github.smling.proxmoxmcpserver.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.smling.proxmoxmcpserver.config.Config;
import io.github.smling.proxmoxmcpserver.config.AuthConfig;
import io.github.smling.proxmoxmcpserver.config.ProxmoxConfig;
import io.github.smling.proxmoxmcpserver.core.ProxmoxClient;
import io.github.smling.proxmoxmcpserver.core.ProxmoxManager;
import io.github.smling.proxmoxmcpserver.mcp.ProxmoxMcpTools;
import io.github.smling.proxmoxmcpserver.tools.BackupTools;
import io.github.smling.proxmoxmcpserver.tools.ClusterTools;
import io.github.smling.proxmoxmcpserver.tools.ContainerTools;
import io.github.smling.proxmoxmcpserver.tools.IsoTools;
import io.github.smling.proxmoxmcpserver.tools.NodeTools;
import io.github.smling.proxmoxmcpserver.tools.SnapshotTools;
import io.github.smling.proxmoxmcpserver.tools.StorageTools;
import io.github.smling.proxmoxmcpserver.tools.VmTools;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.ai.tool.ToolCallbackProvider;

class ProxmoxConfigurationTests {

    @TempDir
    Path tempDir;

    @Test
    void proxmoxConfigLoadsFromSystemProperty() throws Exception {
        Path configPath = tempDir.resolve("config.json");
        Files.writeString(configPath, """
            {
              "proxmox": {
                "host": "pve.local"
              },
              "auth": {
                "user": "root@pam",
                "token_name": "token",
                "token_value": "secret"
              }
            }
            """);
        System.setProperty("proxmox.mcp.config", configPath.toString());
        try {
            ProxmoxConfiguration configuration = new ProxmoxConfiguration();
            Config config = configuration.proxmoxConfig();

            assertThat(config.getProxmox().getHost()).isEqualTo("pve.local");
        } finally {
            System.clearProperty("proxmox.mcp.config");
        }
    }

    @Test
    void proxmoxConfigLoadsFromEnvVar() throws Exception {
        Path configPath = tempDir.resolve("config-env.json");
        Files.writeString(configPath, """
            {
              "proxmox": {
                "host": "pve.local"
              },
              "auth": {
                "user": "root@pam",
                "token_name": "token",
                "token_value": "secret"
              }
            }
            """);
        ProxmoxConfiguration configuration = new ProxmoxConfiguration(
            key -> "PROXMOX_MCP_CONFIG".equals(key) ? configPath.toString() : null
        );
        Config config = configuration.proxmoxConfig();

        assertThat(config.getProxmox().getHost()).isEqualTo("pve.local");
    }

    @Test
    void toolBeansUseManagerApi() throws Exception {
        ProxmoxConfiguration configuration = new ProxmoxConfiguration();
        ProxmoxManager manager = mock(ProxmoxManager.class);
        ProxmoxClient api = mock(ProxmoxClient.class);
        when(manager.getApi()).thenReturn(api);

        NodeTools nodeTools = configuration.nodeTools(manager);
        VmTools vmTools = configuration.vmTools(manager);
        StorageTools storageTools = configuration.storageTools(manager);
        ClusterTools clusterTools = configuration.clusterTools(manager);
        ContainerTools containerTools = configuration.containerTools(manager);
        SnapshotTools snapshotTools = configuration.snapshotTools(manager);
        IsoTools isoTools = configuration.isoTools(manager);
        BackupTools backupTools = configuration.backupTools(manager);

        assertThat(extractProxmox(nodeTools)).isSameAs(api);
        assertThat(extractProxmox(vmTools)).isSameAs(api);
        assertThat(extractProxmox(storageTools)).isSameAs(api);
        assertThat(extractProxmox(clusterTools)).isSameAs(api);
        assertThat(extractProxmox(containerTools)).isSameAs(api);
        assertThat(extractProxmox(snapshotTools)).isSameAs(api);
        assertThat(extractProxmox(isoTools)).isSameAs(api);
        assertThat(extractProxmox(backupTools)).isSameAs(api);
    }

    @Test
    void proxmoxManagerBuildsFromConfig() {
        ProxmoxConfiguration configuration = new ProxmoxConfiguration();
        ProxmoxConfig proxmoxConfig = new ProxmoxConfig();
        proxmoxConfig.setHost("pve.local");
        AuthConfig authConfig = new AuthConfig();
        authConfig.setUser("root@pam");
        authConfig.setTokenName("token");
        authConfig.setTokenValue("secret");
        Config config = new Config();
        config.setProxmox(proxmoxConfig);
        config.setAuth(authConfig);

        try (MockedConstruction<ProxmoxManager> mocked = Mockito.mockConstruction(ProxmoxManager.class)) {
            ProxmoxManager manager = configuration.proxmoxManager(config);

            assertThat(manager).isSameAs(mocked.constructed().get(0));
        }
    }

    @Test
    void proxmoxToolCallbacksBuildProvider() {
        ProxmoxConfiguration configuration = new ProxmoxConfiguration();
        ProxmoxMcpTools tools = mock(ProxmoxMcpTools.class);

        ToolCallbackProvider provider = configuration.proxmoxToolCallbacks(tools);

        assertThat(provider).isNotNull();
    }

    @Test
    void proxmoxConfigRespectsDefaults() throws Exception {
        ProxmoxConfiguration configuration = new ProxmoxConfiguration();
        ProxmoxConfig proxmoxConfig = new ProxmoxConfig();
        proxmoxConfig.setHost("pve.local");
        Config config = new Config();
        config.setProxmox(proxmoxConfig);

        assertThat(config.getProxmox().getPort()).isEqualTo(8006);
    }

    private static ProxmoxClient extractProxmox(Object tool) throws Exception {
        Field field = tool.getClass().getSuperclass().getDeclaredField("proxmox");
        field.setAccessible(true);
        return (ProxmoxClient) field.get(tool);
    }

}
