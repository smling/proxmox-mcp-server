package io.github.smling.proxmoxmcpserver.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.smling.proxmoxmcpserver.TestSupport;
import io.github.smling.proxmoxmcpserver.core.ProxmoxClient;
import it.corsinvest.proxmoxve.api.Result;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProxmoxToolTests {

    @Test
    void formatResponseHandlesNodeList() {
        TestTool tool = new TestTool(mock(ProxmoxClient.class));
        String output = tool.callFormatResponse(List.of(
            Map.of(
                "node", "pve1",
                "status", "online",
                "uptime", 1L,
                "maxcpu", "2",
                "memory", Map.of("used", 0L, "total", 0L)
            )
        ), "nodes");

        assertThat(output).contains("Proxmox Nodes");
        assertThat(output).contains("Node: pve1");
    }

    @Test
    void formatResponseHandlesNodeStatus() {
        TestTool tool = new TestTool(mock(ProxmoxClient.class));
        String output = tool.callFormatResponse(
            new ProxmoxTool.NodeStatusPayload("pve1", Map.of(
                "status", "online",
                "uptime", 0L,
                "maxcpu", "2",
                "memory", Map.of("used", 0L, "total", 0L)
            )),
            "node_status"
        );

        assertThat(output).contains("Node: pve1");
        assertThat(output).contains("Status: ONLINE");
    }

    @Test
    void formatResponseHandlesVmList() {
        TestTool tool = new TestTool(mock(ProxmoxClient.class));
        String output = tool.callFormatResponse(List.of(
            Map.of(
                "vmid", "100",
                "name", "vm1",
                "status", "running",
                "node", "pve1",
                "cpus", "2",
                "memory", Map.of("used", 0L, "total", 0L)
            )
        ), "vms");

        assertThat(output).contains("Virtual Machines");
        assertThat(output).contains("VM: vm1");
    }

    @Test
    void formatResponseHandlesStorageList() {
        TestTool tool = new TestTool(mock(ProxmoxClient.class));
        String output = tool.callFormatResponse(List.of(
            Map.of(
                "storage", "local",
                "status", "online",
                "type", "dir",
                "used", 0L,
                "total", 0L
            )
        ), "storage");

        assertThat(output).contains("Storage Pools");
        assertThat(output).contains("Storage: local");
    }

    @Test
    void formatResponseHandlesContainerList() {
        TestTool tool = new TestTool(mock(ProxmoxClient.class));
        String output = tool.callFormatResponse(List.of(), "containers");

        assertThat(output).contains("No containers found");
    }

    @Test
    void formatResponseHandlesClusterStatus() {
        TestTool tool = new TestTool(mock(ProxmoxClient.class));
        String output = tool.callFormatResponse(Map.of(
            "name", "cluster",
            "quorum", true,
            "nodes", 2,
            "resources", List.of()
        ), "cluster");

        assertThat(output).contains("Proxmox Cluster");
        assertThat(output).contains("Name: cluster");
    }

    @Test
    void formatResponseFallsBackToJson() {
        TestTool tool = new TestTool(mock(ProxmoxClient.class));
        String output = tool.callFormatResponse(Map.of("ok", true), "unknown");
        assertThat(output).contains("\"ok\" : true");
    }

    @Test
    void formatResponseFallsBackToToStringWhenJsonFails() {
        TestTool tool = new TestTool(mock(ProxmoxClient.class));
        Map<String, Object> data = new HashMap<>();
        data.put("self", data);

        String output = tool.callFormatResponse(data, "unknown");

        assertThat(output).contains("self");
    }

    @ParameterizedTest
    @CsvSource({
        "not found,Resource not found",
        "permission denied,Permission denied",
        "invalid,Invalid input"
    })
    void handleErrorMapsKnownMessages(String message, String expected) {
        TestTool tool = new TestTool(mock(ProxmoxClient.class));
        assertThatThrownBy(() -> tool.callHandleError("op", new RuntimeException(message)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(expected);
    }

    @Test
    void handleErrorThrowsRuntimeForUnknown() {
        TestTool tool = new TestTool(mock(ProxmoxClient.class));
        assertThatThrownBy(() -> tool.callHandleError("op", new RuntimeException("boom")))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to op");
    }

    @Test
    void responseDataReturnsMissingNodeWhenResponseNull() {
        TestTool tool = new TestTool(mock(ProxmoxClient.class));
        Result result = TestSupport.resultWithResponse(null);
        JsonNode node = tool.callResponseData(result);

        assertThat(node.isMissingNode()).isTrue();
    }

    @Test
    void responseDataReturnsDataNode() {
        TestTool tool = new TestTool(mock(ProxmoxClient.class));
        ObjectNode data = TestSupport.mapper().createObjectNode().put("value", 1);
        Result result = TestSupport.resultWithData(data);
        JsonNode node = tool.callResponseData(result);

        assertThat(node.path("value").asInt()).isEqualTo(1);
    }

    private static final class TestTool extends ProxmoxTool {
        TestTool(ProxmoxClient proxmox) {
            super(proxmox);
        }

        String callFormatResponse(Object data, String type) {
            return formatResponse(data, type);
        }

        void callHandleError(String operation, Exception error) {
            handleError(operation, error);
        }

        JsonNode callResponseData(Result result) {
            return responseData(result);
        }
    }
}
