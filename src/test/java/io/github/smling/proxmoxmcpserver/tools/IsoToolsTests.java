package io.github.smling.proxmoxmcpserver.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.smling.proxmoxmcpserver.TestSupport;
import io.github.smling.proxmoxmcpserver.core.ProxmoxClient;
import org.junit.jupiter.api.Test;

class IsoToolsTests {

    private final ObjectMapper mapper = TestSupport.mapper();

    @Test
    void listIsosReturnsEmptyMessage() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        IsoTools tools = new IsoTools(proxmox);

        seedStorageContent(proxmox, "iso", mapper.createArrayNode());

        String output = tools.listIsos(null, null);

        assertThat(output).contains("No ISO images found");
    }

    @Test
    void listIsosIncludesFiltersInEmptyMessage() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        IsoTools tools = new IsoTools(proxmox);

        seedStorageContent(proxmox, "iso", mapper.createArrayNode());

        String output = tools.listIsos("pve1", "local");

        assertThat(output).contains("No ISO images found on node pve1");
        assertThat(output).contains("in storage local");
    }

    @Test
    void listIsosRendersEntries() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        IsoTools tools = new IsoTools(proxmox);

        ArrayNode content = mapper.createArrayNode();
        ObjectNode iso = mapper.createObjectNode();
        iso.put("volid", "local:iso/test.iso");
        iso.put("size", 2048);
        content.add(iso);

        seedStorageContent(proxmox, "iso", content);

        String output = tools.listIsos(null, null);

        assertThat(output).contains("Available ISO Images");
        assertThat(output).contains("test.iso");
    }

    @Test
    void listTemplatesRendersEntries() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        IsoTools tools = new IsoTools(proxmox);

        ArrayNode content = mapper.createArrayNode();
        ObjectNode tmpl = mapper.createObjectNode();
        tmpl.put("volid", "local:vztmpl/template.tar.zst");
        tmpl.put("size", 1024);
        content.add(tmpl);

        seedStorageContent(proxmox, "vztmpl", content);

        String output = tools.listTemplates(null, null);

        assertThat(output).contains("Available OS Templates");
        assertThat(output).contains("template.tar.zst");
    }

    @Test
    void listTemplatesReturnsEmptyMessage() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        IsoTools tools = new IsoTools(proxmox);

        seedStorageContent(proxmox, "vztmpl", mapper.createArrayNode());

        String output = tools.listTemplates("pve1", "local");

        assertThat(output).contains("No OS templates found");
    }

    @Test
    void downloadIsoReturnsSummary() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        IsoTools tools = new IsoTools(proxmox);

        when(proxmox.postForm(eq("/nodes/pve1/storage/local/download-url"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.downloadIso("pve1", "local", "http://example", "file.iso", "sum", "sha256");

        assertThat(output).contains("ISO Download Started");
        assertThat(output).contains("Task ID:");
        assertThat(output).contains("TASK");
    }

    @Test
    void downloadIsoSkipsChecksumWhenMissing() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        IsoTools tools = new IsoTools(proxmox);

        when(proxmox.postForm(eq("/nodes/pve1/storage/local/download-url"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.downloadIso("pve1", "local", "http://example", "file.iso", null, null);

        assertThat(output).doesNotContain("Checksum:");
    }

    @Test
    void deleteIsoResolvesVolidAndReturnsSummary() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        IsoTools tools = new IsoTools(proxmox);

        ArrayNode content = mapper.createArrayNode();
        ObjectNode item = mapper.createObjectNode();
        item.put("volid", "local:iso/file.iso");
        content.add(item);

        when(proxmox.get("/nodes/pve1/storage/local/content"))
            .thenReturn(TestSupport.resultWithData(content));
        when(proxmox.delete("/nodes/pve1/storage/local/content/local:iso/file.iso"))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.deleteIso("pve1", "local", "file.iso");

        assertThat(output).contains("ISO/Template Deleted");
        assertThat(output).contains("local:iso/file.iso");
    }

    @Test
    void deleteIsoReturnsErrorWhenMissing() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        IsoTools tools = new IsoTools(proxmox);

        ArrayNode content = mapper.createArrayNode();
        when(proxmox.get("/nodes/pve1/storage/local/content"))
            .thenReturn(TestSupport.resultWithData(content));

        String output = tools.deleteIso("pve1", "local", "file.iso");

        assertThat(output).contains("Could not find");
    }

    @Test
    void deleteIsoUsesVolidWhenProvided() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        IsoTools tools = new IsoTools(proxmox);

        when(proxmox.delete("/nodes/pve1/storage/local/content/local:iso/file.iso"))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.deleteIso("pve1", "local", "local:iso/file.iso");

        assertThat(output).contains("local:iso/file.iso");
    }

    @Test
    void errorPayloadFallsBackWhenJsonFails() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        IsoTools tools = new IsoTools(proxmox);
        ObjectMapper original = IsoTools.swapObjectMapper(new ObjectMapper() {
            @Override
            public ObjectWriter writerWithDefaultPrettyPrinter() {
                throw new RuntimeException("boom");
            }
        });
        try {
            when(proxmox.get("/nodes")).thenThrow(new RuntimeException("boom"));

            String output = tools.listIsos(null, null);

            assertThat(output).contains("\"error\":\"Failed to list nodes: boom\"");
        } finally {
            IsoTools.swapObjectMapper(original);
        }
    }

    private void seedStorageContent(ProxmoxClient proxmox, String contentType, ArrayNode content) throws Exception {
        ArrayNode nodes = mapper.createArrayNode();
        nodes.add(mapper.createObjectNode().put("node", "pve1"));

        ArrayNode storage = mapper.createArrayNode();
        ObjectNode store = mapper.createObjectNode();
        store.put("storage", "local");
        store.put("content", contentType);
        storage.add(store);

        when(proxmox.get("/nodes")).thenReturn(TestSupport.resultWithData(nodes));
        when(proxmox.get("/nodes/pve1/storage")).thenReturn(TestSupport.resultWithData(storage));
        when(proxmox.get(eq("/nodes/pve1/storage/local/content"), anyMap()))
            .thenReturn(TestSupport.resultWithData(content));
    }

}
