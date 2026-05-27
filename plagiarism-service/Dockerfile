FROM gradle:8.10-jdk21-alpine AS builder

WORKDIR /app

# Копируем только файлы, нужные для зависимостей
COPY settings.gradle build.gradle gradle.properties* ./
COPY gradle/ ./gradle/
COPY gradlew .
COPY common/build.gradle ./common/
COPY security-spring-boot-starter/build.gradle ./security-spring-boot-starter/
COPY plagiarism-service/build.gradle ./plagiarism-service/

# Скачиваем зависимости
RUN ./gradlew dependencies --no-daemon

# Копируем исходники и собираем
COPY common/ ./common/
COPY plagiarism-service/ ./plagiarism-service/
COPY security-spring-boot-starter/ ./security-spring-boot-starter/
RUN ./gradlew :plagiarism-service:bootJar --no-daemon -x test

# Финальная стадия
FROM eclipse-temurin:21-jre-alpine

# Создаем non-root пользователя
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app
COPY --from=builder /app/plagiarism-service/build/libs/*.jar app.jar

# Переключаемся на non-root пользователя
USER appuser

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE 8086

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8086/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]