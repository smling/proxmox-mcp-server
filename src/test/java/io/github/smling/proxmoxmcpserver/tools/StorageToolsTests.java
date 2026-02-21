package io.github.smling.proxmoxmcpserver.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.smling.proxmoxmcpserver.TestSupport;
import io.github.smling.proxmoxmcpserver.core.ProxmoxClient;
import org.junit.jupiter.api.Test;

class StorageToolsTests {

    private final ObjectMapper mapper = TestSupport.mapper();

    @Test
    void getStorageUsesStatusDetailsWhenAvailable() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        StorageTools tools = new StorageTools(proxmox);

        ArrayNode stores = mapper.createArrayNode();
        ObjectNode store = mapper.createObjectNode();
        store.put("storage", "local");
        store.put("node", "pve1");
        store.put("type", "dir");
        store.put("content", "images");
        store.put("enabled", true);
        stores.add(store);

        ObjectNode status = mapper.createObjectNode();
        status.put("used", 1);
        status.put("total", 2);
        status.put("avail", 1);

        when(proxmox.get("/storage")).thenReturn(TestSupport.resultWithData(stores));
        when(proxmox.get("/nodes/pve1/storage/local/status")).thenReturn(TestSupport.resultWithData(status));

        String output = tools.getStorage();

        assertThat(output).contains("Storage: local");
        assertThat(output).contains("Status: ONLINE");
    }

    @Test
    void getStorageFallsBackWhenStatusFails() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        StorageTools tools = new StorageTools(proxmox);

        ArrayNode stores = mapper.createArrayNode();
        ObjectNode store = mapper.createObjectNode();
        store.put("storage", "local");
        store.put("node", "pve1");
        store.put("type", "dir");
        store.put("content", "images");
        store.put("enabled", false);
        stores.add(store);

        when(proxmox.get("/storage")).thenReturn(TestSupport.resultWithData(stores));
        when(proxmox.get("/nodes/pve1/storage/local/status")).thenThrow(new RuntimeException("boom"));

        String output = tools.getStorage();

        assertThat(output).contains("Storage: local");
        assertThat(output).contains("Status: OFFLINE");
    }

    @Test
    void getStorageDefaultsNodeWhenMissing() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        StorageTools tools = new StorageTools(proxmox);

        ArrayNode stores = mapper.createArrayNode();
        ObjectNode store = mapper.createObjectNode();
        store.put("storage", "local");
        store.put("type", "dir");
        store.put("content", "images");
        store.put("enabled", true);
        stores.add(store);

        ObjectNode status = mapper.createObjectNode();
        status.put("used", 1);
        status.put("total", 2);
        status.put("avail", 1);

        when(proxmox.get("/storage")).thenReturn(TestSupport.resultWithData(stores));
        when(proxmox.get("/nodes/localhost/storage/local/status")).thenReturn(TestSupport.resultWithData(status));

        String output = tools.getStorage();

        assertThat(output).contains("Storage: local");
        assertThat(output).contains("Status: ONLINE");
    }
}
