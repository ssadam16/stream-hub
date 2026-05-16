FROM gradle:8.10-jdk21-alpine AS builder

WORKDIR /app

# Копируем только файлы, нужные для зависимостей
COPY settings.gradle build.gradle gradle.properties* ./
COPY gradle/ ./gradle/
COPY gradlew .
COPY common/build.gradle ./common/
COPY code-executor-service/build.gradle ./code-executor-service/

# Скачиваем зависимости
RUN ./gradlew dependencies --no-daemon

COPY code-executor-service/ ./code-executor-service/
COPY common/ ./common/

RUN ./gradlew :code-executor-service:bootJar --no-daemon -x test

# Финальная стадия
FROM eclipse-temurin:21-jre-alpine

# Создаем non-root пользователя
#RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app
COPY --from=builder /app/code-executor-service/build/libs/*.jar app.jar

# Переключаемся на non-root пользователя
#USER appuser

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE 8085

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8085/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]