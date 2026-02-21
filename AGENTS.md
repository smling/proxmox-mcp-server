# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/io/github/smling` contains the Spring Boot entry point (`ProxmoxMcpServerApplication.java`) and future application code.
- `src/main/resources/application.yaml` holds default configuration (currently the app name).
- `src/test/java/io/github/smling` contains JUnit tests, including the Spring context smoke test.
- `pom.xml` defines dependencies and build configuration; `mvnw`/`mvnw.cmd` are the Maven wrappers.

## Build, Test, and Development Commands
Use the Maven wrapper to keep builds consistent:
- `./mvnw spring-boot:run` (or `mvnw.cmd spring-boot:run` on Windows) starts the app locally.
- `./mvnw test` runs the JUnit test suite.
- `./mvnw package` builds the application JAR.

## Coding Style & Naming Conventions
- Java 21 is the target version; keep code compatible with it.
- Follow standard Java conventions: 4-space indentation, braces on the same line, and organized imports.
- Package names should stay under `io.github.smling`.
- Class names use `UpperCamelCase`; test classes end with `Tests` (example: `ProxmoxMcpServerApplicationTests`).
- No formatter or linter is configured yet, so keep style consistent with existing files.

## Testing Guidelines
- Tests use JUnit Jupiter via Spring Boot test starters.
- Place tests in `src/test/java` mirroring the main package structure.
- There is no explicit coverage gate; add focused tests for new endpoints or security changes.

## Commit & Pull Request Guidelines
- Git history only shows an `initial commit`, so there is no established commit convention yet.
- Prefer short, imperative commit messages (example: `Add health endpoint`) and keep unrelated changes split.
- Pull requests should include a clear description, test evidence (commands run), and linked issues when applicable.

## Configuration & Security Notes
- Keep secrets out of `application.yaml`; use environment variables or a local override file not tracked by git.
- This project includes Spring Security and Actuator dependencies; document any changes that affect authentication or exposed endpoints.
