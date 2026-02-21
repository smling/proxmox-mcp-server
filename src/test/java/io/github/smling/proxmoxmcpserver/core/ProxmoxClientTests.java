package io.github.smling.proxmoxmcpserver.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.corsinvest.proxmoxve.api.PveClient;
import it.corsinvest.proxmoxve.api.Result;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;

class ProxmoxClientTests {

    @Test
    void exposesBaseUriAndAuthHeader() {
        ProxmoxClient client = new ProxmoxClient("pve.local", 8006, true, "root@pam", "token", "secret");
        URI baseUri = client.getBaseUri();

        assertThat(baseUri.toString()).isEqualTo("https://pve.local:8006/api2/json");
        assertThat(client.getAuthHeaderValue()).isEqualTo("PVEAPIToken root@pam!token=secret");
    }

    @Test
    void testConnectionThrowsOnFailedStatus() throws Exception {
        ProxmoxClient client = new ProxmoxClient("pve.local", 8006, true, "root@pam", "token", "secret");
        PveClient pveClient = mock(PveClient.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        Result result = mock(Result.class);
        when(result.isSuccessStatusCode()).thenReturn(false);
        when(result.getReasonPhrase()).thenReturn("Denied");
        when(pveClient.getVersion().version()).thenReturn(result);
        setField(client, "client", pveClient);

        assertThatThrownBy(client::testConnection)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Denied");
    }

    @Test
    void testConnectionSucceedsWithSuccessStatus() throws Exception {
        ProxmoxClient client = new ProxmoxClient("pve.local", 8006, true, "root@pam", "token", "secret");
        PveClient pveClient = mock(PveClient.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        Result result = mock(Result.class);
        when(result.isSuccessStatusCode()).thenReturn(true);
        when(pveClient.getVersion().version()).thenReturn(result);
        setField(client, "client", pveClient);

        client.testConnection();
    }

    @Test
    void testConnectionThrowsWhenResultNull() throws Exception {
        ProxmoxClient client = new ProxmoxClient("pve.local", 8006, true, "root@pam", "token", "secret");
        PveClient pveClient = mock(PveClient.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(pveClient.getVersion().version()).thenReturn(null);
        setField(client, "client", pveClient);

        assertThatThrownBy(client::testConnection)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No response");
    }

    @Test
    void getReturnsResultOnSuccess() throws Exception {
        ProxmoxClient client = new ProxmoxClient("pve.local", 8006, true, "root@pam", "token", "secret");
        PveClient pveClient = mock(PveClient.class);
        Result result = mock(Result.class);
        when(result.isSuccessStatusCode()).thenReturn(true);
        when(result.responseInError()).thenReturn(false);
        when(pveClient.get(eq("/nodes"), any())).thenReturn(result);
        setField(client, "client", pveClient);

        assertThat(client.get("/nodes")).isSameAs(result);
    }

    @Test
    void postFormReturnsResultOnSuccess() throws Exception {
        ProxmoxClient client = new ProxmoxClient("pve.local", 8006, true, "root@pam", "token", "secret");
        PveClient pveClient = mock(PveClient.class);
        Result result = mock(Result.class);
        when(result.isSuccessStatusCode()).thenReturn(true);
        when(result.responseInError()).thenReturn(false);
        when(pveClient.create(eq("/nodes/pve/qemu"), any())).thenReturn(result);
        setField(client, "client", pveClient);

        assertThat(client.postForm("/nodes/pve/qemu", Map.of("a", "b"))).isSameAs(result);
    }

    @Test
    void putFormReturnsResultOnSuccess() throws Exception {
        ProxmoxClient client = new ProxmoxClient("pve.local", 8006, true, "root@pam", "token", "secret");
        PveClient pveClient = mock(PveClient.class);
        Result result = mock(Result.class);
        when(result.isSuccessStatusCode()).thenReturn(true);
        when(result.responseInError()).thenReturn(false);
        when(pveClient.set(eq("/nodes/pve/qemu/100/config"), any())).thenReturn(result);
        setField(client, "client", pveClient);

        assertThat(client.putForm("/nodes/pve/qemu/100/config", Map.of("cpu", "2"))).isSameAs(result);
    }

    @Test
    void deleteReturnsResultOnSuccess() throws Exception {
        ProxmoxClient client = new ProxmoxClient("pve.local", 8006, true, "root@pam", "token", "secret");
        PveClient pveClient = mock(PveClient.class);
        Result result = mock(Result.class);
        when(result.isSuccessStatusCode()).thenReturn(true);
        when(result.responseInError()).thenReturn(false);
        when(pveClient.delete(eq("/nodes/pve/qemu/100"), eq(null))).thenReturn(result);
        setField(client, "client", pveClient);

        assertThat(client.delete("/nodes/pve/qemu/100")).isSameAs(result);
    }

    @Test
    void getThrowsOnErrorStatus() throws Exception {
        ProxmoxClient client = new ProxmoxClient("pve.local", 8006, true, "root@pam", "token", "secret");
        PveClient pveClient = mock(PveClient.class);
        Result result = mock(Result.class);
        when(result.isSuccessStatusCode()).thenReturn(false);
        when(result.getStatusCode()).thenReturn(401);
        when(result.getReasonPhrase()).thenReturn("Unauthorized");
        when(pveClient.get(eq("/nodes"), any())).thenReturn(result);
        setField(client, "client", pveClient);

        assertThatThrownBy(() -> client.get("/nodes"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("401");
    }

    @Test
    void getThrowsOnErrorPayload() throws Exception {
        ProxmoxClient client = new ProxmoxClient("pve.local", 8006, true, "root@pam", "token", "secret");
        PveClient pveClient = mock(PveClient.class);
        Result result = mock(Result.class);
        when(result.isSuccessStatusCode()).thenReturn(true);
        when(result.responseInError()).thenReturn(true);
        when(result.getError()).thenReturn("bad");
        when(pveClient.get(eq("/nodes"), any())).thenReturn(result);
        setField(client, "client", pveClient);

        assertThatThrownBy(() -> client.get("/nodes"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("bad");
    }

    @Test
    void getThrowsWhenResultNull() throws Exception {
        ProxmoxClient client = new ProxmoxClient("pve.local", 8006, true, "root@pam", "token", "secret");
        PveClient pveClient = mock(PveClient.class);
        when(pveClient.get(eq("/nodes"), any())).thenReturn(null);
        setField(client, "client", pveClient);

        assertThatThrownBy(() -> client.get("/nodes"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("no response");
    }

    @Test
    void exchangeRejectsUnsupportedMethod() throws Exception {
        ProxmoxClient client = new ProxmoxClient("pve.local", 8006, true, "root@pam", "token", "secret");
        Method exchange = ProxmoxClient.class.getDeclaredMethod("exchange", String.class, HttpMethod.class,
            Map.class, Map.class);
        exchange.setAccessible(true);

        assertThatThrownBy(() -> exchange.invoke(client, "/nodes", HttpMethod.PATCH, Map.of(), null))
            .hasRootCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toObjectMapFiltersNullValues() throws Exception {
        Method toObjectMap = ProxmoxClient.class.getDeclaredMethod("toObjectMap", Map.class);
        toObjectMap.setAccessible(true);
        Map<String, String> input = new java.util.HashMap<>();
        input.put("a", "1");
        input.put("b", null);

        Object result = toObjectMap.invoke(null, input);

        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertThat(resultMap).containsEntry("a", "1");
        assertThat(resultMap).doesNotContainKey("b");
    }

    @ParameterizedTest
    @MethodSource("nullishMaps")
    void toObjectMapReturnsNullForEmptyValues(Map<String, String> input) throws Exception {
        Method toObjectMap = ProxmoxClient.class.getDeclaredMethod("toObjectMap", Map.class);
        toObjectMap.setAccessible(true);

        Object result = toObjectMap.invoke(null, input);

        assertThat(result).isNull();
    }

    private static Stream<Map<String, String>> nullishMaps() {
        Map<String, String> onlyNulls = new HashMap<>();
        onlyNulls.put("a", null);
        return Stream.of(null, new HashMap<>(), onlyNulls);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
