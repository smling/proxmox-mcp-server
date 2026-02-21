package io.github.smling.proxmoxmcpserver.formatting;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProxmoxTemplatesTests {

    @Test
    void nodeListRendersSummary() {
        List<Map<String, Object>> nodes = List.of(
            Map.of(
                "node", "pve1",
                "status", "online",
                "uptime", 60L,
                "maxcpu", "4",
                "memory", Map.of("used", 1024L, "total", 2048L)
            )
        );

        String output = ProxmoxTemplates.nodeList(nodes);

        assertThat(output).contains("Proxmox Nodes");
        assertThat(output).contains("Node: pve1");
        assertThat(output).contains("Status: ONLINE");
    }

    @Test
    void nodeStatusRendersSummary() {
        String output = ProxmoxTemplates.nodeStatus("pve1", Map.of(
            "status", "offline",
            "uptime", 0L,
            "maxcpu", "N/A",
            "memory", Map.of("used", 0L, "total", 0L)
        ));

        assertThat(output).contains("Node: pve1");
        assertThat(output).contains("Status: OFFLINE");
    }

    @Test
    void vmListRendersSummary() {
        List<Map<String, Object>> vms = List.of(
            Map.of(
                "name", "vm1",
                "vmid", "100",
                "status", "running",
                "node", "pve1",
                "cpus", "2",
                "memory", Map.of("used", 1024L, "total", 2048L)
            )
        );

        String output = ProxmoxTemplates.vmList(vms);

        assertThat(output).contains("Virtual Machines");
        assertThat(output).contains("VM: vm1 (ID: 100)");
    }

    @Test
    void storageListRendersSummary() {
        List<Map<String, Object>> storage = List.of(
            Map.of(
                "storage", "local",
                "status", "online",
                "type", "dir",
                "used", 1024L,
                "total", 2048L
            )
        );

        String output = ProxmoxTemplates.storageList(storage);

        assertThat(output).contains("Storage Pools");
        assertThat(output).contains("Storage: local");
    }

    @Test
    void containerListHandlesEmptyList() {
        assertThat(ProxmoxTemplates.containerList(List.of()))
            .isEqualTo("No containers found");
    }

    @Test
    void containerListRendersEntriesWithFallbackMemory() {
        List<Map<String, Object>> containers = List.of(
            Map.of(
                "name", "ct1",
                "vmid", "101",
                "status", "running",
                "node", "pve1",
                "cpus", "2",
                "memory", "n/a"
            )
        );

        String output = ProxmoxTemplates.containerList(containers);

        assertThat(output).contains("Container: ct1");
        assertThat(output).contains("Memory: 0.00 B / 0.00 B");
    }

    @Test
    void clusterStatusRendersSummary() {
        String output = ProxmoxTemplates.clusterStatus(Map.of(
            "name", "cluster",
            "quorum", true,
            "nodes", 2,
            "resources", List.of(Map.of("id", "qemu/100"))
        ));

        assertThat(output).contains("Proxmox Cluster");
        assertThat(output).contains("Name: cluster");
        assertThat(output).contains("Quorum: OK");
        assertThat(output).contains("Resources: 1");
    }

    @Test
    void clusterStatusHandlesNonListResourcesAndNotOk() {
        String output = ProxmoxTemplates.clusterStatus(Map.of(
            "name", "cluster",
            "quorum", "no",
            "nodes", 1,
            "resources", "n/a"
        ));

        assertThat(output).contains("Quorum: NOT OK");
        assertThat(output).doesNotContain("Resources:");
    }

    @Test
    void constructorIsPrivate() throws Exception {
        Constructor<ProxmoxTemplates> ctor = ProxmoxTemplates.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(ctor.getModifiers())).isTrue();
        ctor.setAccessible(true);
        assertThat(ctor.newInstance()).isNotNull();
    }
}
