# Proxmox MCP Server

**Overview**
🚀 Proxmox MCP Server is a Spring Boot service that exposes Proxmox VE operations as MCP tools using Spring AI. It loads a JSON config at startup, validates the Proxmox API connection, and serves a set of tools for day-to-day cluster automation.

🧰 Highlights:
- Nodes and cluster status
- VMs lifecycle and guest command execution
- LXC containers lifecycle and resource updates
- Storage pools, ISO/templates, and backups
- Snapshots and rollbacks

**How To Use**
1. 🧩 Create a Proxmox MCP config JSON (keep real credentials out of git):

```json
{
  "proxmox": {
    "host": "proxmox.example.local",
    "port": 8006,
    "verify_ssl": true,
    "service": "PVE"
  },
  "auth": {
    "user": "api-user@pam",
    "token_name": "proxmox-mcp-server",
    "token_value": "REPLACE_WITH_TOKEN"
  },
  "mcp": {
    "host": "127.0.0.1",
    "port": 8000,
    "transport": "STDIO"
  }
}
```

2. 🔐 Point the app to the config file:

```powershell
$env:PROXMOX_MCP_CONFIG="C:\path\to\proxmox-mcp.json"
```

```bash
export PROXMOX_MCP_CONFIG=/path/to/proxmox-mcp.json
```

You can also use a system property instead of the env var:

```bash
java -Dproxmox.mcp.config=/path/to/proxmox-mcp.json -jar target/proxmox-mcp-server.jar
```

3. ▶️ Run locally with the Maven wrapper:

```bash
./mvnw spring-boot:run
```

```powershell
mvnw.cmd spring-boot:run
```

4. 🐳 Run in Docker (optional):

```bash
docker build -t proxmox-mcp-server .
docker run --rm --name proxmox-mcp -p 8080:8080 -v "/path/to/proxmox-mcp.json:/config/proxmox-mcp.json:ro" proxmox-mcp-server
```

**Contributing**
✅ Keep code compatible with Java 21 and keep packages under `io.github.smling`.  
✅ Add tests in `src/test/java` and use parameterized tests where possible; keep happy and unhappy paths in separate methods.  
✅ Run tests with `./mvnw test` (or `mvnw.cmd test` on Windows) and note the command in your PR.  
✅ Keep secrets out of tracked files; use env vars or local config overrides.  
✅ Follow logging guidelines: debug for traceability, info for Proxmox changes/results, warning for non-blocking issues, and error with stack traces for failures.  
