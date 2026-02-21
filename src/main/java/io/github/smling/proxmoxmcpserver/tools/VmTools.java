package io.github.smling.proxmoxmcpserver.tools;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.smling.proxmoxmcpserver.core.ProxmoxClient;
import io.github.smling.proxmoxmcpserver.formatting.ProxmoxFormatters;
import io.github.smling.proxmoxmcpserver.tools.console.VmConsoleManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Virtual machine operations for Proxmox.
 */
public class VmTools extends ProxmoxTool {
    private final VmConsoleManager consoleManager;

    /**
     * Creates VM tools with a Proxmox client.
     *
     * @param proxmox the Proxmox client
     */
    public VmTools(ProxmoxClient proxmox) {
        super(proxmox);
        this.consoleManager = new VmConsoleManager(proxmox);
    }

    /**
     * Lists virtual machines across the cluster.
     *
     * @return formatted VM list
     */
    public String getVms() {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            JsonNode nodes = responseData(proxmox.get("/nodes"));
            for (JsonNode node : nodes) {
                String nodeName = node.path("node").asText(null);
                if (nodeName == null) {
                    logger.warn("Skipping unexpected node entry while gathering VM list: {}", node);
                    continue;
                }
                JsonNode vms;
                try {
                    vms = responseData(proxmox.get("/nodes/" + nodeName + "/qemu"));
                } catch (Exception nodeError) {
                    logger.warn("Skipping node {} while gathering VM list", nodeName, nodeError);
                    continue;
                }

                for (JsonNode vm : vms) {
                    String vmid = vm.path("vmid").asText();
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("vmid", vmid);
                    entry.put("name", vm.path("name").asText());
                    entry.put("status", vm.path("status").asText());
                    entry.put("node", nodeName);
                    entry.put("memory", Map.of(
                        "used", vm.path("mem").asLong(0),
                        "total", vm.path("maxmem").asLong(0)
                    ));

                    try {
                        JsonNode config = responseData(
                            proxmox.get("/nodes/" + nodeName + "/qemu/" + vmid + "/config")
                        );
                        entry.put("cpus", config.path("cores").asText("N/A"));
                    } catch (Exception ignored) {
                        entry.put("cpus", "N/A");
                    }
                    result.add(entry);
                }
            }
        } catch (Exception e) {
            handleError("get VMs", e);
        }

        return formatResponse(result, "vms");
    }

    /**
     * Creates a new VM with the supplied configuration.
     *
     * @param node host node name
     * @param vmid VM ID
     * @param name VM name
     * @param cpus CPU core count
     * @param memory memory size in MB
     * @param diskSize disk size in GB
     * @param storage storage pool name
     * @param ostype OS type
     * @param networkBridge network bridge name
     * @return creation status message
     */
    public String createVm(
        String node,
        String vmid,
        String name,
        int cpus,
        int memory,
        int diskSize,
        String storage,
        String ostype,
        String networkBridge
    ) {
        try {
            try {
                proxmox.get("/nodes/" + node + "/qemu/" + vmid + "/config");
                throw new IllegalArgumentException("VM " + vmid + " already exists on node " + node);
            } catch (Exception e) {
                if (e.getMessage() != null && !e.getMessage().toLowerCase().contains("not found")) {
                    throw e;
                }
            }

            JsonNode storageList = responseData(proxmox.get("/nodes/" + node + "/storage"));
            Map<String, JsonNode> storageInfo = new HashMap<>();
            for (JsonNode store : storageList) {
                storageInfo.put(store.path("storage").asText(), store);
            }

            if (storage == null || storage.isBlank()) {
                storage = pickStorage(storageList, "local-lvm");
                if (storage == null) {
                    storage = pickStorage(storageList, "vm-storage");
                }
                if (storage == null) {
                    for (JsonNode store : storageList) {
                        if (store.path("content").asText("").contains("images")) {
                            storage = store.path("storage").asText();
                            break;
                        }
                    }
                }
                if (storage == null) {
                    throw new IllegalArgumentException("No suitable storage found for VM images");
                }
            }

            JsonNode selectedStorage = storageInfo.get(storage);
            if (selectedStorage == null) {
                throw new IllegalArgumentException("Storage '" + storage + "' not found on node " + node);
            }
            if (!selectedStorage.path("content").asText("").contains("images")) {
                throw new IllegalArgumentException("Storage '" + storage + "' does not support VM images");
            }

            String storageType = selectedStorage.path("type").asText("unknown");
            Map<String, String> vmStorage = new HashMap<>();
            String diskFormat;
            if ("lvm".equals(storageType) || "lvmthin".equals(storageType)) {
                diskFormat = "raw";
                vmStorage.put("scsi0", storage + ":" + diskSize + ",format=" + diskFormat);
            } else if ("dir".equals(storageType) || "nfs".equals(storageType) || "cifs".equals(storageType)) {
                diskFormat = "qcow2";
                vmStorage.put("scsi0", storage + ":" + diskSize + ",format=" + diskFormat);
                vmStorage.put("ide2", storage + ":cloudinit");
            } else {
                diskFormat = "raw";
                vmStorage.put("scsi0", storage + ":" + diskSize + ",format=" + diskFormat);
            }

            if (ostype == null || ostype.isBlank()) {
                ostype = "l26";
            }
            if (networkBridge == null || networkBridge.isBlank()) {
                networkBridge = "vmbr0";
            }

            Map<String, String> vmConfig = new HashMap<>();
            vmConfig.put("vmid", vmid);
            vmConfig.put("name", name);
            vmConfig.put("cores", String.valueOf(cpus));
            vmConfig.put("memory", String.valueOf(memory));
            vmConfig.put("ostype", ostype);
            vmConfig.put("scsihw", "virtio-scsi-pci");
            vmConfig.put("boot", "order=scsi0");
            vmConfig.put("agent", "1");
            vmConfig.put("vga", "std");
            vmConfig.put("net0", "virtio,bridge=" + networkBridge);
            vmConfig.putAll(vmStorage);

            JsonNode taskResult = responseData(proxmox.postForm("/nodes/" + node + "/qemu", vmConfig));
            String cloudinitNote = "";
            if ("lvm".equals(storageType) || "lvmthin".equals(storageType)) {
                cloudinitNote = "\n  Note: LVM storage does not support cloud-init image";
            }

            String resultText = "VM " + vmid + " created successfully.\n\n"
                + "VM Configuration:\n"
                + "  Name: " + name + "\n"
                + "  Node: " + node + "\n"
                + "  VM ID: " + vmid + "\n"
                + "  CPU Cores: " + cpus + "\n"
                + "  Memory: " + memory + " MB (" + String.format("%.1f", memory / 1024.0) + " GB)\n"
                + "  Disk: " + diskSize + " GB (" + storage + ", " + diskFormat + " format)\n"
                + "  Storage Type: " + storageType + "\n"
                + "  OS Type: " + ostype + "\n"
                + "  Network: virtio (bridge=" + networkBridge + ")\n"
                + "  QEMU Agent: Enabled" + cloudinitNote + "\n\n"
                + "Task ID: " + taskResult + "\n\n"
                + "Next steps:\n"
                + "  1. Upload an ISO to install the operating system\n"
                + "  2. Start the VM using startVm\n"
                + "  3. Access the console to complete OS installation";

            return resultText;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            handleError("create VM " + vmid, e);
            return "";
        }
    }

    /**
     * Starts a VM.
     *
     * @param node host node name
     * @param vmid VM ID
     * @return start status message
     */
    public String startVm(String node, String vmid) {
        try {
            JsonNode status = responseData(proxmox.get("/nodes/" + node + "/qemu/" + vmid + "/status/current"));
            if ("running".equalsIgnoreCase(status.path("status").asText())) {
                return "VM " + vmid + " is already running";
            }
            JsonNode task = responseData(
                proxmox.postForm("/nodes/" + node + "/qemu/" + vmid + "/status/start", Map.of())
            );
            return "VM " + vmid + " start initiated successfully\nTask ID: " + task;
        } catch (Exception e) {
            if (messageHasNotFound(e)) {
                throw new IllegalArgumentException("VM " + vmid + " not found on node " + node);
            }
            handleError("start VM " + vmid, e);
            return "";
        }
    }

    /**
     * Stops a VM.
     *
     * @param node host node name
     * @param vmid VM ID
     * @return stop status message
     */
    public String stopVm(String node, String vmid) {
        try {
            JsonNode status = responseData(proxmox.get("/nodes/" + node + "/qemu/" + vmid + "/status/current"));
            if ("stopped".equalsIgnoreCase(status.path("status").asText())) {
                return "VM " + vmid + " is already stopped";
            }
            JsonNode task = responseData(
                proxmox.postForm("/nodes/" + node + "/qemu/" + vmid + "/status/stop", Map.of())
            );
            return "VM " + vmid + " stop initiated successfully\nTask ID: " + task;
        } catch (Exception e) {
            if (messageHasNotFound(e)) {
                throw new IllegalArgumentException("VM " + vmid + " not found on node " + node);
            }
            handleError("stop VM " + vmid, e);
            return "";
        }
    }

    /**
     * Shuts down a VM gracefully.
     *
     * @param node host node name
     * @param vmid VM ID
     * @return shutdown status message
     */
    public String shutdownVm(String node, String vmid) {
        try {
            JsonNode status = responseData(proxmox.get("/nodes/" + node + "/qemu/" + vmid + "/status/current"));
            if ("stopped".equalsIgnoreCase(status.path("status").asText())) {
                return "VM " + vmid + " is already stopped";
            }
            JsonNode task = responseData(
                proxmox.postForm("/nodes/" + node + "/qemu/" + vmid + "/status/shutdown", Map.of())
            );
            return "VM " + vmid + " graceful shutdown initiated\nTask ID: " + task;
        } catch (Exception e) {
            if (messageHasNotFound(e)) {
                throw new IllegalArgumentException("VM " + vmid + " not found on node " + node);
            }
            handleError("shutdown VM " + vmid, e);
            return "";
        }
    }

    /**
     * Resets a VM.
     *
     * @param node host node name
     * @param vmid VM ID
     * @return reset status message
     */
    public String resetVm(String node, String vmid) {
        try {
            JsonNode status = responseData(proxmox.get("/nodes/" + node + "/qemu/" + vmid + "/status/current"));
            if ("stopped".equalsIgnoreCase(status.path("status").asText())) {
                return "Cannot reset VM " + vmid + ": VM is currently stopped\nUse startVm to start it first";
            }
            JsonNode task = responseData(
                proxmox.postForm("/nodes/" + node + "/qemu/" + vmid + "/status/reset", Map.of())
            );
            return "VM " + vmid + " reset initiated successfully\nTask ID: " + task;
        } catch (Exception e) {
            if (messageHasNotFound(e)) {
                throw new IllegalArgumentException("VM " + vmid + " not found on node " + node);
            }
            handleError("reset VM " + vmid, e);
            return "";
        }
    }

    /**
     * Executes a command inside a VM via the guest agent.
     *
     * @param node host node name
     * @param vmid VM ID
     * @param command command to execute
     * @return formatted command output
     */
    public String executeCommand(String node, String vmid, String command) {
        try {
            Map<String, Object> result = consoleManager.executeCommand(node, vmid, command);
            boolean success = Boolean.TRUE.equals(result.get("success"));
            String output = result.get("output") == null ? "" : result.get("output").toString();
            String error = result.get("error") == null ? "" : result.get("error").toString();
            return ProxmoxFormatters.formatCommandOutput(success, command, output, error);
        } catch (Exception e) {
            handleError("execute command on VM " + vmid, e);
            return "";
        }
    }

    /**
     * Deletes a VM, optionally stopping it first.
     *
     * @param node host node name
     * @param vmid VM ID
     * @param force force deletion even if running
     * @return deletion status message
     */
    public String deleteVm(String node, String vmid, boolean force) {
        try {
            JsonNode status = responseData(proxmox.get("/nodes/" + node + "/qemu/" + vmid + "/status/current"));
            String currentStatus = status.path("status").asText();
            String vmName = status.path("name").asText("VM-" + vmid);

            StringBuilder result = new StringBuilder();
            if ("running".equalsIgnoreCase(currentStatus)) {
                if (!force) {
                    throw new IllegalArgumentException("VM " + vmid + " (" + vmName + ") is currently running. "
                        + "Please stop it first or use force=true to stop and delete.");
                }
                proxmox.postForm("/nodes/" + node + "/qemu/" + vmid + "/status/stop", Map.of());
                result.append("Stopping VM ").append(vmid).append(" (").append(vmName).append(") before deletion...\n");
            } else {
                result.append("Deleting VM ").append(vmid).append(" (").append(vmName).append(")...\n");
            }

            JsonNode task = responseData(proxmox.delete("/nodes/" + node + "/qemu/" + vmid));
            result.append("VM ").append(vmid).append(" (").append(vmName).append(") deletion initiated successfully.\n\n")
                .append("WARNING: This operation will permanently remove:\n")
                .append("  VM configuration\n")
                .append("  All virtual disks\n")
                .append("  All snapshots\n")
                .append("  Cannot be undone!\n\n")
                .append("Task ID: ").append(task).append("\n\n")
                .append("VM ").append(vmid).append(" (").append(vmName).append(") is being deleted from node ").append(node);

            return result.toString();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            if (messageHasNotFound(e)) {
                throw new IllegalArgumentException("VM " + vmid + " not found on node " + node);
            }
            handleError("delete VM " + vmid, e);
            return "";
        }
    }

    /**
     * Selects a storage pool by name when it supports VM images.
     *
     * @param storageList the storage list
     * @param name the preferred storage name
     * @return the selected storage name or {@code null}
     */
    private String pickStorage(JsonNode storageList, String name) {
        for (JsonNode store : storageList) {
            if (name.equals(store.path("storage").asText()) && store.path("content").asText("").contains("images")) {
                return name;
            }
        }
        return null;
    }

    /**
     * Checks if an exception message indicates a missing resource.
     *
     * @param e the exception to inspect
     * @return {@code true} when the message suggests a not-found error
     */
    private boolean messageHasNotFound(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("does not exist") || lower.contains("not found");
    }
}
