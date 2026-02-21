package io.github.smling.proxmoxmcpserver.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smling.proxmoxmcpserver.config.AuthConfig;
import io.github.smling.proxmoxmcpserver.config.ProxmoxConfig;
import java.lang.reflect.Method;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

class ProxmoxManagerTests {

    @Test
    void getApiReturnsConfiguredClient() throws Exception {
        ProxmoxClient client = mock(ProxmoxClient.class);
        ProxmoxManager manager = new ProxmoxManager(client, false);

        assertThat(manager.getApi()).isSameAs(client);
    }

    @Test
    void constructorValidatesConnectionWhenEnabled() throws Exception {
        ProxmoxClient client = mock(ProxmoxClient.class);
        when(client.getBaseUri()).thenReturn(URI.create("https://pve.local"));

        ProxmoxManager manager = new ProxmoxManager(client, true);

        verify(client).testConnection();
        assertThat(manager.getApi()).isSameAs(client);
    }

    @Test
    void constructorBuildsClientFromConfig() {
        ProxmoxConfig proxmoxConfig = new ProxmoxConfig();
        proxmoxConfig.setHost("pve.local");
        proxmoxConfig.setPort(8006);
        proxmoxConfig.setVerifySsl(true);

        AuthConfig authConfig = new AuthConfig();
        authConfig.setUser("root@pam");
        authConfig.setTokenName("token");
        authConfig.setTokenValue("secret");

        try (MockedConstruction<ProxmoxClient> mocked = Mockito.mockConstruction(ProxmoxClient.class,
            (mock, context) -> when(mock.getBaseUri()).thenReturn(URI.create("https://pve.local")))) {
            ProxmoxManager manager = new ProxmoxManager(proxmoxConfig, authConfig);

            assertThat(manager.getApi()).isSameAs(mocked.constructed().get(0));
        }
    }

    @Test
    void testConnectionReturnsClientOnSuccess() throws Exception {
        ProxmoxClient client = mock(ProxmoxClient.class);
        when(client.getBaseUri()).thenReturn(URI.create("https://pve.local"));
        ProxmoxManager manager = new ProxmoxManager(client, false);

        Method method = ProxmoxManager.class.getDeclaredMethod("testConnection", ProxmoxClient.class);
        method.setAccessible(true);
        Object result = method.invoke(manager, client);

        assertThat(result).isSameAs(client);
    }

    @Test
    void testConnectionWrapsFailure() throws Exception {
        ProxmoxClient client = mock(ProxmoxClient.class);
        when(client.getBaseUri()).thenReturn(URI.create("https://pve.local"));
        doThrow(new IllegalStateException("boom")).when(client).testConnection();
        ProxmoxManager manager = new ProxmoxManager(client, false);

        Method method = ProxmoxManager.class.getDeclaredMethod("testConnection", ProxmoxClient.class);
        method.setAccessible(true);

        assertThatThrownBy(() -> method.invoke(manager, client))
            .hasRootCauseInstanceOf(RuntimeException.class);
    }
}
