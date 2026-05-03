# 🚀 Stream Hub

**Stream Hub** — платформа для решения алгоритмических задач с автоматической проверкой кода.  
Пользователь отправляет решение, оно выполняется в изолированном Docker-контейнере, результат прогоняется через тест-кейсы и возвращается с вердиктом: `ACCEPTED`, `WRONG_ANSWER`, `TIME_LIMIT_EXCEEDED`, `RUNTIME_ERROR` и т.д.

Аналог LeetCode/Codeforces — написанный с нуля как микросервисное приложение.

---

## 🏗 Архитектура

```
                        ┌─────────────────┐
                        │   API Gateway   │  :8080
                        │  Spring WebFlux │
                        │  Keycloak OAuth2│
                        └────────┬────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
     ┌────────▼───────┐ ┌────────▼───────┐ ┌───────▼────────┐
     │  user-service  │ │  task-service  │ │solution-service│
     │   :8081        │ │   :8082        │ │   :8084        │
     │   PostgreSQL   │ │   PostgreSQL   │ │   PostgreSQL   │
     └────────────────┘ └────────────────┘ └───────┬────────┘
                                                   │
                                              Kafka │ (3 brokers)
                                                   │
                                          ┌────────▼────────┐
                                          │code-executor-   │
                                          │service  :8085   │
                                          │Docker API       │
                                          │Java/Python/Go/  │
                                          │C++ execution    │
                                          └─────────────────┘
```

**Сервисы:**

| Сервис | Описание | БД |
|---|---|---|
| `api-gateway` | Реактивный gateway (WebFlux), JWT-валидация через Keycloak | — |
| `user-service` | Регистрация, профили пользователей | PostgreSQL |
| `task-service` | CRUD задач, тест-кейсы (консольный и unit-тип) | PostgreSQL |
| `solution-service` | Приём решений, публикация в Kafka, хранение результатов | PostgreSQL + MongoDB |
| `code-executor-service` | Выполнение кода в Docker-контейнерах, поддержка 4 языков | — |
| `config-server` | Централизованная конфигурация (Spring Cloud Config) | — |

---

## ⚙️ Стек технологий

- **Java 17**, Spring Boot 3, Spring WebFlux
- **Spring Cloud Gateway** — реактивный API gateway
- **Keycloak** — аутентификация и авторизация (OAuth2 / JWT)
- **Apache Kafka** (3 брокера, KRaft mode) — асинхронная передача задач на выполнение
- **Docker Java API** — динамический запуск контейнеров для изоляции пользовательского кода
- **PostgreSQL** — основные данные (пользователи, задачи, решения)
- **MongoDB** — логи выполнения кода
- **Gradle** (multi-module project)
- **Docker Compose** — полный локальный запуск всей инфраструктуры

---

## 🔐 Аутентификация

Аутентификация реализована через **Keycloak**:
- Пользователь логинится через `api-gateway → Keycloak`, получает JWT
- Gateway проверяет подпись токена локально (публичный ключ Keycloak) — без похода в Keycloak на каждый запрос
- Роли (`ADMIN`, `USER`) пробрасываются в downstream-сервисы через заголовки

---

## ⚡ Выполнение кода

Ключевая часть проекта — **code-executor-service**:

1. `solution-service` принимает решение от пользователя и публикует событие в Kafka-топик
2. `code-executor-service` читает событие, определяет язык через `LanguageExecutorFactory`
3. Создаётся изолированный Docker-контейнер с лимитами CPU и памяти
4. Код компилируется (если нужно) и запускается против каждого тест-кейса
5. Результат (статус + время выполнения) публикуется обратно в Kafka
6. `solution-service` обновляет статус решения в БД

**Поддерживаемые языки:** Java 17, Python 3.11, Go 1.21, C++17

**Статусы выполнения:**
- `COMPLETED` — все тесты пройдены
- `PARTIALLY_COMPLETED` — часть тестов пройдена
- `WRONG_ANSWER` — неверный ответ
- `TIME_LIMIT_EXCEEDED` — превышен лимит времени
- `RUNTIME_ERROR` — ошибка во время выполнения
- `COMPILATION_ERROR` — ошибка компиляции
- `INTERNAL_ERROR` — внутренняя ошибка сервиса

---

## 🚀 Быстрый старт

### Требования
- Docker + Docker Compose
- JDK 17+

### Запуск

1. Склонировать репозиторий:
```bash
git clone https://gitlab.com/nongrataqqq/stream-hub.git
cd stream-hub
```

2. Создать `.env` файл:
```env
DB_PASSWORD=your_password
KEYCLOAK_CLIENT_SECRET=your_keycloak_secret
CLUSTER_ID=your_kafka_cluster_id
BIND_PATH=/tmp/code-execution
MONGO_ROOT_USERNAME=root
MONGO_ROOT_PASSWORD=root123
```

3. Запустить всю инфраструктуру:
```bash
docker-compose up --build
```

4. Сервисы будут доступны:

| Сервис | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| Keycloak Admin | http://localhost:8090 |
| Kafka UI | http://localhost:8989 |
| Mongo Express | http://localhost:8081 |

---

## 📁 Структура проекта

```
stream-hub/
├── api-gateway/              # Реактивный gateway + Keycloak JWT
├── user-service/             # Управление пользователями
├── task-service/             # Задачи и тест-кейсы
├── solution-service/         # Приём решений + Kafka producer/consumer
├── code-executor-service/    # Docker-изоляция + выполнение кода
├── common/                   # Общие DTO, события Kafka, исключения
├── config-server/            # Spring Cloud Config Server
└── docker-compose.yaml       # Полная инфраструктура
```

---

## 🗺 Планы по развитию

- [ ] Добавить ELK Stack (Elasticsearch + Logstash + Kibana) для централизованного логирования
- [ ] Покрыть code-executor-service интеграционными тестами (Testcontainers)
- [ ] Добавить рейтинговую систему пользователей
- [ ] Реализовать поддержку unit-тестов как критерия проверки (помимо консольного I/O)
