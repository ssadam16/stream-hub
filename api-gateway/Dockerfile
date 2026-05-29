FROM gradle:8.14-jdk21-alpine AS builder

WORKDIR /app

COPY settings.gradle build.gradle gradle.properties* ./
COPY gradle/ ./gradle/
COPY common/build.gradle ./common/
COPY api-gateway/build.gradle ./api-gateway/

RUN gradle dependencies --no-daemon

COPY common/ ./common/
COPY api-gateway/ ./api-gateway/
RUN gradle :api-gateway:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=builder /app/api-gateway/build/libs/*.jar app.jar
USER appuser
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]