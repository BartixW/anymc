FROM gradle:9.0-jdk21 AS builder

WORKDIR /app

COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY gradlew ./

COPY anymc-servers/build.gradle.kts ./anymc-servers/
COPY anymc-core/build.gradle.kts ./anymc-core/
COPY anymc-minecraft-core/build.gradle.kts ./anymc-minecraft-core/

COPY anymc-servers/src ./anymc-servers/src
COPY anymc-core/src ./anymc-core/src
COPY anymc-minecraft-core/src ./anymc-minecraft-core/src

RUN --mount=type=cache,target=/root/.gradle ./gradlew :anymc-servers:dependencies --no-daemon
RUN --mount=type=cache,target=/root/.gradle ./gradlew :anymc-servers:shadowJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN mkdir -p /app/data

COPY --from=builder /app/anymc-servers/build/libs/anymc-servers.jar /app/app.jar

ENV PORT=8080 \
    ANYMC_DATA_DIR=/app/data

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]