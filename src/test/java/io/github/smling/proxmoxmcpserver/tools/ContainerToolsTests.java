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
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

class ContainerToolsTests {

    private final ObjectMapper mapper = TestSupport.mapper();

    @Test
    void getContainersRendersPrettyOutput() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        ArrayNode nodes = mapper.createArrayNode();
        nodes.add(mapper.createObjectNode().put("node", "pve1"));

        ArrayNode containers = mapper.createArrayNode();
        ObjectNode ct = mapper.createObjectNode();
        ct.put("vmid", 101);
        ct.put("name", "ct1");
        ct.put("status", "running");
        containers.add(ct);

        when(proxmox.get("/nodes")).thenReturn(TestSupport.resultWithData(nodes));
        when(proxmox.get("/nodes/pve1/lxc")).thenReturn(TestSupport.resultWithData(containers));

        String output = tools.getContainers(null, false, false, "pretty");

        assertThat(output).contains("Containers");
        assertThat(output).contains("ct1 (ID: 101)");
    }

    @Test
    void getContainersIncludesStatsInJsonOutput() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        ArrayNode nodes = mapper.createArrayNode();
        nodes.add(mapper.createObjectNode().put("node", "pve1"));

        ArrayNode containers = mapper.createArrayNode();
        ObjectNode ct = mapper.createObjectNode();
        ct.put("vmid", 101);
        ct.put("name", "ct1");
        ct.put("status", "running");
        containers.add(ct);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "running");
        status.put("cpu", 0.0);
        status.put("mem", 0);
        status.put("maxmem", 0);

        ObjectNode config = mapper.createObjectNode();
        config.put("cores", 2);
        config.put("memory", 256);
        config.put("swap", 0);

        ArrayNode rrd = mapper.createArrayNode();
        ObjectNode rrdSample = mapper.createObjectNode();
        rrdSample.put("cpu", 0.5);
        rrdSample.put("mem", 1024);
        rrdSample.put("maxmem", 2048);
        rrd.add(rrdSample);

        when(proxmox.get("/nodes")).thenReturn(TestSupport.resultWithData(nodes));
        when(proxmox.get("/nodes/pve1/lxc")).thenReturn(TestSupport.resultWithData(containers));
        when(proxmox.get("/nodes/pve1/lxc/101/status/current")).thenReturn(TestSupport.resultWithData(status));
        when(proxmox.get("/nodes/pve1/lxc/101/config")).thenReturn(TestSupport.resultWithData(config));
        when(proxmox.get(eq("/nodes/pve1/lxc/101/rrddata"), anyMap()))
            .thenReturn(TestSupport.resultWithData(rrd));

        String output = tools.getContainers(null, true, false, "json");

        assertThat(output).contains("\"cpu_pct\" : 50.0");
        assertThat(output).contains("\"mem_bytes\" : 1024");
    }

    @Test
    void getContainersUsesNodeFilterAndHandlesNumericIds() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        ArrayNode containers = mapper.createArrayNode();
        containers.add(mapper.getNodeFactory().numberNode(101));

        when(proxmox.get("/nodes/pve1/lxc")).thenReturn(TestSupport.resultWithData(containers));

        String output = tools.getContainers("pve1", false, false, "pretty");

        assertThat(output).contains("ct-101 (ID: 101)");
    }

    @Test
    void getContainersIncludesStatsAndRawForStoppedContainer() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        seedInventory(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "stopped");
        status.put("cpu", 0.0);
        status.put("mem", 0);
        status.put("maxmem", 0);

        ObjectNode config = mapper.createObjectNode();
        config.put("memory", 512);
        config.put("swap", 0);
        config.put("cpulimit", 2.5);

        ArrayNode rrd = mapper.createArrayNode();
        ObjectNode rrdSample = mapper.createObjectNode();
        rrdSample.put("cpu", 0.1);
        rrdSample.put("mem", 2048);
        rrdSample.put("maxmem", 4096);
        rrd.add(rrdSample);

        when(proxmox.get("/nodes/pve1/lxc/101/status/current")).thenReturn(TestSupport.resultWithData(status));
        when(proxmox.get("/nodes/pve1/lxc/101/config")).thenReturn(TestSupport.resultWithData(config));
        when(proxmox.get(eq("/nodes/pve1/lxc/101/rrddata"), anyMap()))
            .thenReturn(TestSupport.resultWithData(rrd));

        String output = tools.getContainers(null, true, true, "pretty");

        assertThat(output).contains("ct1 (ID: 101)");
        assertThat(output).contains("CPU Cores: 2.5");
    }

    @Test
    void getContainersHandlesUnlimitedMemory() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        seedInventory(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "running");
        status.put("cpu", 0.0);
        status.put("mem", 1024);
        status.put("maxmem", 0);

        ObjectNode config = mapper.createObjectNode();
        config.put("memory", 0);
        config.put("swap", 0);
        config.put("cores", 1);

        when(proxmox.get("/nodes/pve1/lxc/101/status/current")).thenReturn(TestSupport.resultWithData(status));
        when(proxmox.get("/nodes/pve1/lxc/101/config")).thenReturn(TestSupport.resultWithData(config));
        when(proxmox.get(eq("/nodes/pve1/lxc/101/rrddata"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.createArrayNode()));

        String output = tools.getContainers(null, true, false, "pretty");

        assertThat(output).contains("unlimited");
    }

    @Test
    void getContainersHandlesRrdErrors() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        seedInventory(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "running");
        status.put("cpu", 0.0);
        status.put("mem", 0);
        status.put("maxmem", 0);

        ObjectNode config = mapper.createObjectNode();
        config.put("memory", 0);
        config.put("swap", 0);

        when(proxmox.get("/nodes/pve1/lxc/101/status/current")).thenReturn(TestSupport.resultWithData(status));
        when(proxmox.get("/nodes/pve1/lxc/101/config")).thenReturn(TestSupport.resultWithData(config));
        when(proxmox.get(eq("/nodes/pve1/lxc/101/rrddata"), anyMap()))
            .thenThrow(new RuntimeException("boom"));

        String output = tools.getContainers(null, true, false, "pretty");

        assertThat(output).contains("Containers");
    }

    @Test
    void getContainersHandlesZeroMaxMemory() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        seedInventory(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "running");
        status.put("cpu", 0.0);
        status.put("mem", 0);
        status.put("maxmem", 0);

        ObjectNode config = mapper.createObjectNode();
        config.put("memory", 0);
        config.put("swap", 512);

        when(proxmox.get("/nodes/pve1/lxc/101/status/current")).thenReturn(TestSupport.resultWithData(status));
        when(proxmox.get("/nodes/pve1/lxc/101/config")).thenReturn(TestSupport.resultWithData(config));
        when(proxmox.get(eq("/nodes/pve1/lxc/101/rrddata"), anyMap()))
            .thenThrow(new RuntimeException("boom"));

        String output = tools.getContainers(null, true, false, "pretty");

        assertThat(output).contains("Memory: 0.00 B / 0.00 B");
    }

    @Test
    void startContainerPerformsAction() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        seedInventory(proxmox);

        when(proxmox.postForm(eq("/nodes/pve1/lxc/101/status/start"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.startContainer("pve1:101", "pretty");

        assertThat(output).contains("Start Containers");
        assertThat(output).contains("OK");
    }

    @Test
    void startContainerReturnsErrorWhenSelectorMissing() {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        String output = tools.startContainer(" ", "pretty");

        assertThat(output).contains("\"error\"");
    }

    @Test
    void startContainerReportsFailureWhenActionThrows() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        seedInventory(proxmox);
        when(proxmox.postForm(eq("/nodes/pve1/lxc/101/status/start"), anyMap()))
            .thenThrow(new RuntimeException("boom"));

        String output = tools.startContainer("101", "pretty");

        assertThat(output).contains("FAIL");
    }

    @Test
    void startContainerSkipsInvalidSelectorTokens() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        seedInventory(proxmox);

        String output = tools.startContainer("pve1:bad", "pretty");

        assertThat(output).contains("No containers matched");
    }

    @Test
    void stopContainerUsesForceStopWhenNotGraceful() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        seedInventory(proxmox);

        when(proxmox.postForm(eq("/nodes/pve1/lxc/101/status/stop"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.stopContainer("101", false, 10, "pretty");

        assertThat(output).contains("Stop Containers");
        assertThat(output).contains("OK");
    }

    @Test
    void createContainerReturnsSummary() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        ArrayNode nodes = mapper.createArrayNode();
        nodes.add(mapper.createObjectNode().put("node", "pve1"));
        when(proxmox.get("/nodes")).thenReturn(TestSupport.resultWithData(nodes));

        ArrayNode storage = mapper.createArrayNode();
        storage.add(mapper.createObjectNode().put("storage", "local-lvm").put("content", "rootdir"));
        when(proxmox.get("/storage")).thenReturn(TestSupport.resultWithData(storage));

        when(proxmox.postForm(eq("/nodes/pve1/lxc"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.createContainer("pve1", "101", "tmpl", null, 1, 256, 256, 8,
            null, null, null, "vmbr0", false, true);

        assertThat(output).contains("Container Created Successfully");
        assertThat(output).contains("Task ID:");
        assertThat(output).contains("TASK");
    }

    @Test
    void deleteContainerReportsRunningWithoutForce() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        seedInventory(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "running");
        when(proxmox.get("/nodes/pve1/lxc/101/status/current")).thenReturn(TestSupport.resultWithData(status));

        String output = tools.deleteContainer("101", false, "pretty");

        assertThat(output).contains("FAIL");
        assertThat(output).contains("running");
    }

    @Test
    void deleteContainerStopsWhenForceEnabled() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        seedInventory(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "running");
        when(proxmox.get("/nodes/pve1/lxc/101/status/current")).thenReturn(TestSupport.resultWithData(status));
        when(proxmox.delete("/nodes/pve1/lxc/101"))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.deleteContainer("101", true, "pretty");

        assertThat(output).contains("Stopped and deleted");
    }

    @Test
    void deleteContainerJsonOutputIncludesFailures() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        seedInventory(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "running");
        when(proxmox.get("/nodes/pve1/lxc/101/status/current")).thenReturn(TestSupport.resultWithData(status));

        String output = tools.deleteContainer("101", false, "json");

        assertThat(output).contains("\"ok\" : false");
    }

    @Test
    void updateContainerResourcesReportsChanges() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        seedInventory(proxmox);

        when(proxmox.putForm(eq("/nodes/pve1/lxc/101/config"), anyMap()))
            .thenReturn(TestSupport.resultWithResponse(mapper.createObjectNode()));
        when(proxmox.putForm(eq("/nodes/pve1/lxc/101/resize"), anyMap()))
            .thenReturn(TestSupport.resultWithResponse(mapper.createObjectNode()));

        String output = tools.updateContainerResources("101", 2, 512, 0, 5, "rootfs", "pretty");

        assertThat(output).contains("Update Container Resources");
        assertThat(output).contains("cores=2");
    }

    @Test
    void updateContainerResourcesReportsNoChanges() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        seedInventory(proxmox);

        String output = tools.updateContainerResources("101", null, null, null, null, null, "pretty");

        assertThat(output).contains("no changes");
    }

    @Test
    void updateContainerResourcesUsesJsonFormat() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        seedInventory(proxmox);
        when(proxmox.putForm(eq("/nodes/pve1/lxc/101/config"), anyMap()))
            .thenReturn(TestSupport.resultWithResponse(mapper.createObjectNode()));

        String output = tools.updateContainerResources("101", 2, null, null, null, null, "json");

        assertThat(output).contains("\"ok\" : true");
    }

    @Test
    void createContainerRejectsExistingVmid() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        seedInventory(proxmox);

        String output = tools.createContainer("pve1", "101", "tmpl", null, 1, 256, 256, 8,
            null, null, null, null, false, true);

        assertThat(output).contains("already exists");
    }

    @Test
    void createContainerRejectsMissingNode() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        ArrayNode nodes = mapper.createArrayNode();
        nodes.add(mapper.createObjectNode().put("node", "pve2"));
        when(proxmox.get("/nodes")).thenReturn(TestSupport.resultWithData(nodes));

        String output = tools.createContainer("pve1", "102", "tmpl", null, 1, 256, 256, 8,
            null, null, null, null, false, true);

        assertThat(output).contains("Node 'pve1' not found");
    }

    @Test
    void createContainerIncludesCredentials() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        ArrayNode nodes = mapper.createArrayNode();
        nodes.add(mapper.createObjectNode().put("node", "pve1"));
        when(proxmox.get("/nodes")).thenReturn(TestSupport.resultWithData(nodes));

        ArrayNode storage = mapper.createArrayNode();
        storage.add(mapper.createObjectNode().put("storage", "local-lvm").put("content", "rootdir"));
        when(proxmox.get("/storage")).thenReturn(TestSupport.resultWithData(storage));

        when(proxmox.postForm(eq("/nodes/pve1/lxc"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.createContainer("pve1", "103", "tmpl", "ct1", 2, 512, 512, 8,
            null, "pw", "ssh", null, true, false);

        assertThat(output).contains("Container Created Successfully");

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(proxmox).postForm(eq("/nodes/pve1/lxc"), captor.capture());
        assertThat(captor.getValue()).containsEntry("password", "pw");
        assertThat(captor.getValue()).containsEntry("ssh-public-keys", "ssh");
        assertThat(captor.getValue()).containsEntry("start", "1");
        assertThat(captor.getValue()).containsEntry("unprivileged", "0");
    }

    @ParameterizedTest
    @MethodSource("selectorCases")
    void startContainerResolvesSelectors(String selector) throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);

        seedInventory(proxmox);
        when(proxmox.postForm(eq("/nodes/pve1/lxc/101/status/start"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.startContainer(selector, "pretty");

        assertThat(output).contains("OK");
    }

    @Test
    void errorPayloadFallsBackWhenJsonFails() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        ContainerTools tools = new ContainerTools(proxmox);
        ObjectMapper original = ContainerTools.swapObjectMapper(new ObjectMapper() {
            @Override
            public ObjectWriter writerWithDefaultPrettyPrinter() {
                throw new RuntimeException("boom");
            }
        });
        try {
            when(proxmox.get("/nodes")).thenThrow(new RuntimeException("boom"));

            String output = tools.getContainers(null, false, false, "pretty");

            assertThat(output).contains("\"error\":\"Failed to list containers: boom\"");
        } finally {
            ContainerTools.swapObjectMapper(original);
        }
    }

    private void seedInventory(ProxmoxClient proxmox) throws Exception {
        ArrayNode nodes = mapper.createArrayNode();
        nodes.add(mapper.createObjectNode().put("node", "pve1"));

        ArrayNode containers = mapper.createArrayNode();
        ObjectNode ct = mapper.createObjectNode();
        ct.put("vmid", 101);
        ct.put("name", "ct1");
        containers.add(ct);

        when(proxmox.get("/nodes")).thenReturn(TestSupport.resultWithData(nodes));
        when(proxmox.get("/nodes/pve1/lxc")).thenReturn(TestSupport.resultWithData(containers));
    }

    private static Stream<String> selectorCases() {
        return Stream.of("pve1:101", "pve1/ct1", "101", "ct1", "101,ct1");
    }

}
