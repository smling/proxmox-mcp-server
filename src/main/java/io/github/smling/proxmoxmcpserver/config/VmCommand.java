package io.github.smling.proxmoxmcpserver.config;

/**
 * Request payload for executing a command inside a VM.
 */
public class VmCommand {
    private String node;
    private String vmid;
    private String command;

    /**
     * Returns the target node identifier.
     *
     * @return the node name or ID
     */
    public String getNode() {
        return node;
    }

    /**
     * Sets the target node identifier.
     *
     * @param node the node name or ID
     */
    public void setNode(String node) {
        this.node = node;
    }

    /**
     * Returns the VM identifier.
     *
     * @return the VM ID
     */
    public String getVmid() {
        return vmid;
    }

    /**
     * Sets the VM identifier.
     *
     * @param vmid the VM ID
     */
    public void setVmid(String vmid) {
        this.vmid = vmid;
    }

    /**
     * Returns the command to execute.
     *
     * @return the command string
     */
    public String getCommand() {
        return command;
    }

    /**
     * Sets the command to execute.
     *
     * @param command the command string
     */
    public void setCommand(String command) {
        this.command = command;
    }
}
