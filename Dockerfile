# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get autoremove -y \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -B -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -DskipTests package \
    && JAR_FILE=$(ls target/*.jar | grep -v 'original' | head -n 1) \
    && cp "$JAR_FILE" /workspace/app.jar

FROM eclipse-temurin:21-jre-jammy
ARG OCI_TITLE="proxmox-mcp-server"
ARG OCI_DESCRIPTION="Proxmox MCP Server"
ARG OCI_VERSION="0.0.1-SNAPSHOT"
ARG OCI_SOURCE="unknown"
ARG OCI_REVISION="unknown"
ARG OCI_CREATED="unknown"
ARG OCI_LICENSES="unknown"
ARG OCI_URL="unknown"
ARG OCI_AUTHORS="unknown"
LABEL org.opencontainers.image.title=$OCI_TITLE \
    org.opencontainers.image.description=$OCI_DESCRIPTION \
    org.opencontainers.image.version=$OCI_VERSION \
    org.opencontainers.image.source=$OCI_SOURCE \
    org.opencontainers.image.revision=$OCI_REVISION \
    org.opencontainers.image.created=$OCI_CREATED \
    org.opencontainers.image.licenses=$OCI_LICENSES \
    org.opencontainers.image.url=$OCI_URL \
    org.opencontainers.image.authors=$OCI_AUTHORS
RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get autoremove -y \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system app \
    && useradd --system --uid 10001 --gid app --home /app --shell /usr/sbin/nologin app

WORKDIR /app
RUN mkdir -p /config \
    && chown -R app:app /app /config

COPY --from=build --chown=app:app /workspace/app.jar /app/app.jar

ENV PROXMOX_MCP_CONFIG=/config/proxmox-mcp.json
VOLUME ["/config"]

USER app
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

# Example:
#
# docker build \
#     --build-arg OCI_SOURCE=https://example.com/repo \
#     --build-arg OCI_REVISION=$(git rev-parse --short HEAD) \
#     --build-arg OCI_CREATED=$(date -u +%Y-%m-%dT%H:%M:%SZ) \
#     --build-arg OCI_VERSION=0.0.1 \
#     --build-arg OCI_LICENSES=Apache-2.0 \
#     --build-arg OCI_URL=https://example.com \
#     --build-arg OCI_AUTHORS="Your Name <you@example.com>" \
#     -t proxmox-mcp-server .
# Command:
#
#  docker run --rm --name proxmox-mcp -p 8080:8080 -v "$(Resolve-Path .\proxmox-mcp.json):/config/proxmox-mcp.json:ro" proxmox-mcp-server
