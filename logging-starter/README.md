# logging-starter

Spring Boot Auto-configuration стартер для единого логирования во всех сервисах stream-hub.

## Что делает

- Единый `logback-spring.xml` с `LogstashEncoder` (JSON-логи для Loki/ELK)
- `service` в каждом лог-событии берётся из `spring.application.name`
- Log level через env: `LOGGING_LEVEL_ROOT=DEBUG`
- **MVC-сервисы** — `MdcFilter` (MDC: `traceId`, `spanId`, `userId`, `solutionId`)
- **Gateway** — `ReactiveRequestLoggingFilter` (логирует method, path, status, duration, traceId)
- Условная регистрация через `@ConditionalOnClass` — Gateway не получает servlet-фильтр и наоборот

## Подключение

### 1. settings.gradle (root)
```groovy
include 'logging-starter'
```

### 2. build.gradle каждого сервиса
```groovy
dependencies {
    implementation project(':logging-starter')
    // убрать: implementation 'net.logstash.logback:logstash-logback-encoder:8.0'
}
```

### 3. Удалить из каждого сервиса
- `src/main/resources/logback-spring.xml` — теперь берётся из стартера
- `MDCFilter.java` в task-service — заменяется стартерным
- `RequestLoggingFilter.java` в api-gateway — заменяется стартерным
  (UserContextFilter оставить — он про JWT, не про логирование)

### 4. root build.gradle — убрать дублирование зависимости
```groovy
subprojects {
    dependencies {
        // убрать: implementation 'net.logstash.logback:logstash-logback-encoder:8.0'
        // теперь транзитивно через logging-starter
    }
}
```

## MDC поля

Заголовки, которые должен проставлять api-gateway при проксировании:

| MDC ключ     | Заголовок       |
|-------------|-----------------|
| `traceId`   | `X-Trace-Id`    |
| `spanId`    | `X-Span-Id`     |
| `userId`    | `X-User-Id`     |
| `solutionId`| `X-Solution-Id` |

## Что осталось сделать по задаче SH-18

- [ ] Gateway: добавить `X-Span-Id` и `X-Solution-Id` в `UserContextFilter` (по аналогии с `X-Trace-Id`)
- [ ] Проверить что `spanId` и `solutionId` появляются в JSON-логах в Loki
