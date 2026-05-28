FROM gradle:8.10-jdk21-alpine AS builder

WORKDIR /app

# Копируем только файлы, нужные для зависимостей
COPY settings.gradle build.gradle gradle.properties* ./
COPY gradle/ ./gradle/
COPY gradlew .
RUN chmod +x gradlew
COPY common/build.gradle ./common/
COPY api-gateway/build.gradle ./api-gateway/

# Скачиваем зависимости
RUN ./gradlew dependencies --no-daemon

# Копируем исходники и собираем
COPY common/ ./common/
COPY api-gateway/ ./api-gateway/
RUN ./gradlew :api-gateway:bootJar --no-daemon -x test

# Финальная стадия
FROM eclipse-temurin:21-jre-alpine

# Создаем non-root пользователя
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app
COPY --from=builder /app/api-gateway/build/libs/*.jar app.jar

# Переключаемся на non-root пользователя
USER appuser

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]