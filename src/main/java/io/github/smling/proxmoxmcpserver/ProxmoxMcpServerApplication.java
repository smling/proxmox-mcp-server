package io.github.smling.proxmoxmcpserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the Proxmox MCP server.
 */
@SpringBootApplication
public class ProxmoxMcpServerApplication {

	/**
	 * Bootstraps the Spring application.
	 *
	 * @param args command-line arguments passed to Spring Boot
	 */
	public static void main(String[] args) {
		SpringApplication.run(ProxmoxMcpServerApplication.class, args);
	}

}
