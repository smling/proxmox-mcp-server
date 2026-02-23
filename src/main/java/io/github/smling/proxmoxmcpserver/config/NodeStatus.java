package io.github.smling.proxmoxmcpserver.config;

/**
 * Payload used for node status requests.
 */
public class NodeStatus {
    private String node;

    /**
     * Returns the node identifier.
     *
     * @return the node name or ID
     */
    public String getNode() {
        return node;
    }

    /**
     * Sets the node identifier.
     *
     * @param node the node name or ID
     */
    public void setNode(String node) {
        this.node = node;
    }
}
