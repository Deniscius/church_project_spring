# syntax=docker/dockerfile:1.7
# Missanye API — context: Backend/
# Bonnes pratiques : BuildKit cache, layered JAR (Spring Boot tools), non-root, polices PDF

# ---------------------------------------------------------------------------
# 1) Build Maven (cache ~/.m2 entre les builds)
# ---------------------------------------------------------------------------
FROM maven:3.9.15-eclipse-temurin-26 AS build
WORKDIR /workspace

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests package \
    && mv target/church_project_spring-*.jar /workspace/application.jar

# ---------------------------------------------------------------------------
# 2) Extraction des couches (jarmode=tools, Spring Boot 3.5+)
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-jammy AS extract
WORKDIR /extract
COPY --from=build /workspace/application.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract \
    --layers \
    --destination extracted \
    --application-filename application.jar

# ---------------------------------------------------------------------------
# 3) Runtime minimal
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-jammy AS runtime

# OpenPDF (reçus) : fontconfig + DejaVu ; curl pour HEALTHCHECK
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        fontconfig \
        fonts-dejavu-core \
        curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 spring \
    && useradd --system --uid 10001 --gid spring \
        --home-dir /app --shell /usr/sbin/nologin spring \
    && mkdir -p /app/uploads \
    && chown -R spring:spring /app

WORKDIR /app

# Couches stables d'abord → meilleur cache image
COPY --from=extract --chown=spring:spring /extract/extracted/dependencies/ ./
COPY --from=extract --chown=spring:spring /extract/extracted/spring-boot-loader/ ./
COPY --from=extract --chown=spring:spring /extract/extracted/snapshot-dependencies/ ./
COPY --from=extract --chown=spring:spring /extract/extracted/application/ ./

USER 10001:10001

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    APP_STORAGE_ROOT=/app/uploads \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Render injecte PORT ; Spring : server.port=${PORT:8081}
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS "http://127.0.0.1:${PORT:-8080}/actuator/health" || exit 1

# Forme exec (PID 1 = JVM). extract --layers (Boot 3.3+) → thin application.jar + libs.
ENTRYPOINT ["java", "-jar", "application.jar"]
