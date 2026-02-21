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

class ClusterToolsTests {

    private final ObjectMapper mapper = TestSupport.mapper();

    @Test
    void getClusterStatusBuildsSummary() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ClusterTools tools = new ClusterTools(proxmox);

        ArrayNode status = mapper.createArrayNode();
        ObjectNode first = mapper.createObjectNode();
        first.put("name", "cluster");
        first.put("quorate", true);
        first.put("type", "cluster");
        status.add(first);

        ObjectNode nodeEntry = mapper.createObjectNode();
        nodeEntry.put("type", "node");
        status.add(nodeEntry);

        ObjectNode resourceEntry = mapper.createObjectNode();
        resourceEntry.put("type", "resource");
        resourceEntry.put("id", "qemu/100");
        resourceEntry.put("status", "online");
        status.add(resourceEntry);

        when(proxmox.get("/cluster/status")).thenReturn(TestSupport.resultWithData(status));

        String output = tools.getClusterStatus();

        assertThat(output).contains("Proxmox Cluster");
        assertThat(output).contains("Name: cluster");
        assertThat(output).contains("Nodes: 1");
    }

    @Test
    void getClusterStatusHandlesEmptyStatus() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ClusterTools tools = new ClusterTools(proxmox);

        ArrayNode status = mapper.createArrayNode();
        when(proxmox.get("/cluster/status")).thenReturn(TestSupport.resultWithData(status));

        String output = tools.getClusterStatus();

        assertThat(output).contains("Proxmox Cluster");
        assertThat(output).contains("Nodes: 0");
    }
}
