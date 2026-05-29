FROM gradle:8.14-jdk21-alpine AS builder

WORKDIR /app

COPY settings.gradle build.gradle gradle.properties* ./
COPY gradle/ ./gradle/
COPY common/build.gradle ./common/
COPY security-spring-boot-starter/build.gradle ./security-spring-boot-starter/
COPY solution-service/build.gradle ./solution-service/

RUN gradle dependencies --no-daemon

COPY common/ ./common/
COPY security-spring-boot-starter/ ./security-spring-boot-starter/
COPY solution-service/ ./solution-service/
RUN gradle :solution-service:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=builder /app/solution-service/build/libs/*.jar app.jar
USER appuser
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE 8084
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8084/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]