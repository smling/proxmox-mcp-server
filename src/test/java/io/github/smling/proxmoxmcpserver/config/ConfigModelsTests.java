package io.github.smling.proxmoxmcpserver.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ConfigModelsTests {

    @Test
    void configStartsWithDefaultMcpConfig() {
        Config config = new Config();
        assertThat(config.getMcp()).isNotNull();
    }

    @Test
    void proxmoxConfigHasDefaults() {
        ProxmoxConfig config = new ProxmoxConfig();
        assertThat(config.getPort()).isEqualTo(8006);
        assertThat(config.isVerifySsl()).isTrue();
        assertThat(config.getService()).isEqualTo("PVE");
    }

    @Test
    void mcpConfigHasDefaults() {
        McpConfig config = new McpConfig();
        assertThat(config.getHost()).isEqualTo("127.0.0.1");
        assertThat(config.getPort()).isEqualTo(8000);
        assertThat(config.getTransport()).isEqualTo(McpTransport.STDIO);
    }

    @ParameterizedTest
    @CsvSource({
        "pve.local,8006,true,PVE",
        "pve-alt,8443,false,CUSTOM"
    })
    void proxmoxConfigStoresValues(String host, int port, boolean verifySsl, String service) {
        ProxmoxConfig config = new ProxmoxConfig();
        config.setHost(host);
        config.setPort(port);
        config.setVerifySsl(verifySsl);
        config.setService(service);

        assertThat(config.getHost()).isEqualTo(host);
        assertThat(config.getPort()).isEqualTo(port);
        assertThat(config.isVerifySsl()).isEqualTo(verifySsl);
        assertThat(config.getService()).isEqualTo(service);
    }

    @ParameterizedTest
    @CsvSource({
        "alice,token-a,value-a",
        "bob,token-b,value-b"
    })
    void authConfigStoresValues(String user, String tokenName, String tokenValue) {
        AuthConfig config = new AuthConfig();
        config.setUser(user);
        config.setTokenName(tokenName);
        config.setTokenValue(tokenValue);

        assertThat(config.getUser()).isEqualTo(user);
        assertThat(config.getTokenName()).isEqualTo(tokenName);
        assertThat(config.getTokenValue()).isEqualTo(tokenValue);
    }

    @ParameterizedTest
    @CsvSource({
        "10.0.0.1,7000,stdio,STDIO",
        "0.0.0.0,9000,sse,SSE",
        "127.0.0.1,8001,streamable_http,STREAMABLE"
    })
    void mcpConfigStoresValues(String host, int port, String transport, McpTransport expectedTransport) {
        McpConfig config = new McpConfig();
        config.setHost(host);
        config.setPort(port);
        config.setTransport(transport);

        assertThat(config.getHost()).isEqualTo(host);
        assertThat(config.getPort()).isEqualTo(port);
        assertThat(config.getTransport()).isEqualTo(expectedTransport);
    }

    @ParameterizedTest
    @CsvSource({
        "node-a",
        "node-b"
    })
    void nodeStatusStoresNode(String node) {
        NodeStatus status = new NodeStatus();
        status.setNode(node);
        assertThat(status.getNode()).isEqualTo(node);
    }

    @ParameterizedTest
    @CsvSource({
        "node-a,101,uptime",
        "node-b,202,ls -la"
    })
    void vmCommandStoresValues(String node, String vmid, String command) {
        VmCommand payload = new VmCommand();
        payload.setNode(node);
        payload.setVmid(vmid);
        payload.setCommand(command);

        assertThat(payload.getNode()).isEqualTo(node);
        assertThat(payload.getVmid()).isEqualTo(vmid);
        assertThat(payload.getCommand()).isEqualTo(command);
    }

    @Test
    void configStoresNestedConfigs() {
        Config config = new Config();
        ProxmoxConfig proxmox = new ProxmoxConfig();
        AuthConfig auth = new AuthConfig();
        McpConfig mcp = new McpConfig();

        config.setProxmox(proxmox);
        config.setAuth(auth);
        config.setMcp(mcp);

        assertThat(config.getProxmox()).isSameAs(proxmox);
        assertThat(config.getAuth()).isSameAs(auth);
        assertThat(config.getMcp()).isSameAs(mcp);
    }
}
