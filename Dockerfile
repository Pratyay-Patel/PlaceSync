# ─────────────────────────────────────────────────────────────────────────────
# PlaceSync Backend — Multi-stage Dockerfile
#
# Stage 1 (build): Maven + JDK 21 — resolves dependencies and compiles the JAR
# Stage 2 (runtime): Lean JRE 21 Alpine — runs the JAR as a non-root user
#
# Build:  docker build -t placesync-api .
# Run:    docker run -p 8080:8080 --env-file .env placesync-api
# ─────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy the POM first so Maven dependency resolution is cached separately from
# source code. Rebuilds after source-only changes skip this layer entirely.
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# Copy source and package, skipping tests (tests run in CI, not during image build)
COPY src ./src
RUN mvn package -DskipTests -B --no-transfer-progress

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Create a dedicated non-root system user and group for the application process.
# Running as root inside a container is a security risk.
RUN addgroup -S placesync && adduser -S placesync -G placesync

COPY --from=build /app/target/*.jar app.jar
RUN chown placesync:placesync app.jar

USER placesync

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
