package io.github.smling.proxmoxmcpserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

/**
 * Smoke tests for the Spring Boot application context.
 */
class ProxmoxMcpServerApplicationTests {

	/**
	 * Verifies that the main entry point is present.
	 */
	@Test
	void mainMethodExists() throws Exception {
		Method method = ProxmoxMcpServerApplication.class.getDeclaredMethod("main", String[].class);
		assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
	}

	@Test
	void mainInvokesSpringApplicationRun() {
		try (MockedStatic<SpringApplication> mocked = Mockito.mockStatic(SpringApplication.class)) {
			mocked.when(() -> SpringApplication.run(
				Mockito.eq(ProxmoxMcpServerApplication.class),
				Mockito.<String[]>any()
			)).thenReturn(null);

			ProxmoxMcpServerApplication.main(new String[] {"--test"});

			mocked.verify(() -> SpringApplication.run(
				Mockito.eq(ProxmoxMcpServerApplication.class),
				Mockito.<String[]>any()
			));
		}
	}

}
