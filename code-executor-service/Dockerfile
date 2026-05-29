FROM gradle:8.14-jdk21-alpine AS builder

WORKDIR /app

COPY settings.gradle build.gradle gradle.properties* ./
COPY gradle/ ./gradle/
COPY common/build.gradle ./common/
COPY code-executor-service/build.gradle ./code-executor-service/

RUN gradle dependencies --no-daemon

COPY common/ ./common/
COPY code-executor-service/ ./code-executor-service/
RUN gradle :code-executor-service:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/code-executor-service/build/libs/*.jar app.jar
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE 8085
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8085/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]