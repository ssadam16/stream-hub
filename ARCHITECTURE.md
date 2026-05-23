# stream-hub — cleaned architecture

## DTO layer (common module)

Все DTO в `com.technokratos.common.dto`. Используется единое именование `*View` для read-моделей, `*Request` (в task-service) для write.

### Task DTOs (3 проекции одной сущности)

| DTO              | Кто видит   | Поля                                                                                     |
|------------------|-------------|------------------------------------------------------------------------------------------|
| `TaskAdminView`  | ADMIN       | Всё, включая hidden test cases. `status` (DRAFT/PUBLISHED/ARCHIVED), `authorId`, `condition`, все тесты с `isHidden` и `explanation` |
| `TaskPublicView` | USER        | Только PUBLISHED задачи, тесты без hidden (только примеры)                               |
| `TaskShortView`  | списки + S2S| Краткая карточка: id, title, slug, difficulty, points, stats, tags                       |

### Solution DTOs

| DTO                  | Описание                                                                |
|----------------------|-------------------------------------------------------------------------|
| `SolutionView`       | Полное состояние решения: статус, результаты тестов, код (только owner) |
| `SolutionShortView`  | Карточка для списков: id, taskId, status, language                      |
| `CompletedTaskView`  | Запись в списке «мои завершённые»: TaskShortView + список попыток       |

### Прочее

| DTO            | Назначение                                                                |
|----------------|---------------------------------------------------------------------------|
| `TagDto`       | Тэг: id, name, slug, category                                             |
| `TaskStats`    | Статистика задачи: `totalSubmissions`, `successfulSubmissions`, `acceptanceRate`. Есть фабрика `TaskStats.of(total, success)` |
| `SolutionStats`| Статистика по конкретной задаче (количество сабмитов от всех юзеров)      |
| `UserTaskSummary` | Технический агрегат из JPA-запроса solution-service (внутренний)       |

### Тесты

| DTO              | Используется                                                              |
|------------------|---------------------------------------------------------------------------|
| `TestCasePublic` | В `TaskPublicView` и в `CodeExecutionRequest` (executor только это видит) |
| `TestCaseFull`   | В `TaskAdminView` (с `isHidden` и `explanation`)                          |
| `TestCaseResult` | Результат выполнения одного теста, приходит из executor                   |

### Events (Kafka)

| Event                    | Топик                          | Кто шлёт          | Кто читает                |
|--------------------------|--------------------------------|-------------------|---------------------------|
| `CodeExecutionRequest`   | `code-execution-topic`         | solution-service  | code-executor-service     |
| `CodeExecutionResult`    | `code-execution-results`       | code-executor     | solution-service          |
| `TaskSubmissionEvent`    | `task.statistics.events`       | solution-service  | **task-service** (new!)   |
| `SolutionCompletedEvent` | `solution-completed-events`    | solution-service  | plagiarism-service        |

## Event flow при сабмите решения

```
USER → POST /api/v1/solutions/tasks/{id}/solve  →  solution-service
   1. solution-service создаёт SolutionEntity (status=PROCESSING)
   2. solution-service сохраняет исходник в MongoDB (SolutionSource)
   3. solution-service → GET /internal/v1/tasks/{id} → task-service (TaskAdminView, с hidden тестами)
   4. solution-service → kafka("code-execution-topic", CodeExecutionRequest) → code-executor

code-executor:
   - запускает в Docker, прогоняет тесты
   - kafka("code-execution-results", CodeExecutionResult) → solution-service

solution-service onExecutionResult:
   - обновляет SolutionEntity (status, passed/total, testResults)
   - если статус финальный:
     → kafka("task.statistics.events", TaskSubmissionEvent) → TASK-SERVICE   ← НОВОЕ
   - если COMPLETED:
     → kafka("solution-completed-events", SolutionCompletedEvent) → plagiarism

task-service (TaskStatisticsListener) on TaskSubmissionEvent:
   - taskService.recordSubmission(taskId, accepted)
   - атомарно инкрементит tasks.total_submissions + (accepted ? successful_submissions : 0)
   - acceptanceRate считается налету как successful/total
```

## Endpoint map (task-service)

```
/api/v1/admin/tasks/**       AdminTaskController        требует ROLE_ADMIN
/api/v1/tasks/**             PublicTaskController       USER, видит только PUBLISHED
/api/v1/tags/**              TagController              GET — все, POST/DELETE — ADMIN
/internal/v1/tasks/**        InternalTaskController     service-to-service (gateway не пропускает)
```

## TaskService — единый сервис

Все операции в одном классе: `getPublicById`, `getAdminById`, `listPublic`, `listAdmin`,
`shortViewBatch`, `create`, `update`, `publish`, `delete`, `recordSubmission`.

Различие admin vs public — это проекция через mapper, а не два разных сервиса.

## Что было удалено

- Дубли DTO: `TaskResponse`, `TaskSummary`, `TaskFullDto`, `TaskPublicDto`, `TestCasePayload`,
  `PublicTestCaseDto`, `TestCaseDto`, `SolutionResponse`, `SolutionResultResponse`,
  `SolutionDetailResponse`, `SolutionSummaryResponse`, `MyCompletedTaskResponse`,
  `AdminTaskResponse`, `UserTaskResponse`, `FullTestCaseResponse`, `FullTestResultResponse`,
  `TaskStatisticsResponse`, `DailySubmissionStat`, `TestCaseResponse`, `TaskCreatingRequest`
- Сломанные мапперы: `ConsoleTestCaseMapper` (ссылался на несуществующий класс),
  `TestCaseMapper`, MapStruct-конфиги в task/solution-service. Все мапперы теперь
  hand-written.
- `TestService` / `TestController` / `TestApi` / `ConsoleTestCaseRepository` —
  endpoint'ы, которых не было в реальном флоу.
- Дубли контроллеров: было 3 контроллера для задач (`TaskController`, `UserTaskController`,
  `AdminManagementTaskController`) с `return null;`, теперь 3 чётко разделённых
  по audience.

## Что было добавлено

- `TaskSubmissionEvent` + `TaskStatisticsListener` + `recordSubmission` —
  Kafka pipeline для статистики задач.
- `Tag` entity + `task_tags` join table + Liquibase seed 35 базовых тэгов.
- `TaskStatus` (DRAFT/PUBLISHED/ARCHIVED) + `slug` + `acceptanceRate`.
- `TaskSpecifications` для фильтрации списков по difficulty / tags / status.

## Миграции

Новые Flyway миграции в task-service:
- `V5__create_tags_tables.sql` — tags + task_tags
- `V6__extend_tasks_with_status_slug_counters.sql` — status, slug, счётчики
- `V7__seed_tags.sql` — seed 35 тэгов (Array, DP, Graph, Math, BinarySearch и т.д.)
- `V8__align_columns_and_fix_type_check.sql` — `time_limit` → `time_limit_ms`,
  фикс check constraint у `type` (в V1 была опечатка `'FUNCTION_UNIT, STRESS_TEST'`)

## Что НЕ сделано (для тебя)

- Авторизация по ролям (`@PreAuthorize` / Method Security). Endpoints открыты, gateway
  пробрасывает `X-User-Id` / `X-User-Roles` но проверки не делает.
- Соответствующие endpoint'ы статистики в solution-service (`SolutionClient` в task-service
  их вызывает, но они не реализованы — это нормально, в текущей архитектуре статистика
  обновляется через Kafka, REST endpoint нужен только для fallback'ов).
- user-service: рейтинг, leaderboard, UserStats. Запланировано на этап 2.
- Тесты.

## Сборка

```
./gradlew build
docker compose up -d
```

В этой среде сборку не проверял (network недоступен для Maven Central и services.gradle.org),
проверял типы и импорты статически. Если что-то не соберётся — пиши, скорее всего
будет один-два пропущенных импорта.
