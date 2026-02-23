package io.github.smling.proxmoxmcpserver.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.smling.proxmoxmcpserver.TestSupport;
import io.github.smling.proxmoxmcpserver.core.ProxmoxClient;
import org.junit.jupiter.api.Test;

class NodeToolsTests {

    private final ObjectMapper mapper = TestSupport.mapper();

    @Test
    void getNodesUsesStatusDetailsWhenAvailable() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        NodeTools tools = new NodeTools(proxmox);

        ArrayNode nodes = mapper.createArrayNode();
        ObjectNode node = mapper.createObjectNode();
        node.put("node", "pve1");
        node.put("status", "online");
        node.put("mem", 1024);
        node.put("maxmem", 2048);
        nodes.add(node);

        ObjectNode status = mapper.createObjectNode();
        status.put("uptime", 120);
        status.set("cpuinfo", mapper.createObjectNode().put("cpus", 4));
        status.set("memory", mapper.createObjectNode().put("used", 512).put("total", 2048));

        when(proxmox.get("/nodes")).thenReturn(TestSupport.resultWithData(nodes));
        when(proxmox.get("/nodes/pve1/status")).thenReturn(TestSupport.resultWithData(status));

        String output = tools.getNodes();

        assertThat(output).contains("Node: pve1");
        assertThat(output).contains("CPU Cores: 4");
    }

    @Test
    void getNodesFallsBackWhenStatusFails() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        NodeTools tools = new NodeTools(proxmox);

        ArrayNode nodes = mapper.createArrayNode();
        ObjectNode node = mapper.createObjectNode();
        node.put("node", "pve1");
        node.put("status", "offline");
        node.put("mem", 512);
        node.put("maxmem", 1024);
        nodes.add(node);

        when(proxmox.get("/nodes")).thenReturn(TestSupport.resultWithData(nodes));
        when(proxmox.get("/nodes/pve1/status")).thenThrow(new RuntimeException("boom"));

        String output = tools.getNodes();

        assertThat(output).contains("Node: pve1");
        assertThat(output).contains("Status: OFFLINE");
    }

    @Test
    void getNodeStatusReturnsStatus() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        NodeTools tools = new NodeTools(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "online");
        status.put("uptime", 1);
        status.set("cpuinfo", mapper.createObjectNode().put("cpus", 2));
        status.set("memory", mapper.createObjectNode().put("used", 1).put("total", 2));

        when(proxmox.get("/nodes/pve1/status")).thenReturn(TestSupport.resultWithData(status));

        String output = tools.getNodeStatus("pve1");

        assertThat(output).contains("Node: pve1");
        assertThat(output).contains("Status: ONLINE");
    }

    @Test
    void getNodeStatusFallsBackForOfflineNode() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        NodeTools tools = new NodeTools(proxmox);

        ArrayNode nodes = mapper.createArrayNode();
        ObjectNode node = mapper.createObjectNode();
        node.put("node", "pve1");
        node.put("status", "offline");
        node.put("mem", 1);
        node.put("maxmem", 2);
        nodes.add(node);

        when(proxmox.get("/nodes/pve1/status")).thenThrow(new RuntimeException("boom"));
        when(proxmox.get("/nodes")).thenReturn(TestSupport.resultWithData(nodes));

        String output = tools.getNodeStatus("pve1");

        assertThat(output).contains("Status: OFFLINE");
    }

    @Test
    void getNodeStatusThrowsWhenFallbackFails() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        NodeTools tools = new NodeTools(proxmox);

        when(proxmox.get("/nodes/pve1/status")).thenThrow(new RuntimeException("boom"));
        when(proxmox.get("/nodes")).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> tools.getNodeStatus("pve1"))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getNodeStatusThrowsWhenNodeOnlineAndStatusFails() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        NodeTools tools = new NodeTools(proxmox);

        ArrayNode nodes = mapper.createArrayNode();
        ObjectNode node = mapper.createObjectNode();
        node.put("node", "pve1");
        node.put("status", "online");
        nodes.add(node);

        when(proxmox.get("/nodes/pve1/status")).thenThrow(new RuntimeException("boom"));
        when(proxmox.get("/nodes")).thenReturn(TestSupport.resultWithData(nodes));

        assertThatThrownBy(() -> tools.getNodeStatus("pve1"))
            .isInstanceOf(RuntimeException.class);
    }
}
