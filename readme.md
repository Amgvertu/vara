# Varamy - Хоккейная биржа

Серверная часть приложения для поиска игроков в хоккей, организации матчей и взаимодействия между хоккеистами.

## Технологии

- Java 21 (Eclipse Temurin)
- Spring Boot 3
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL
- Flyway (миграции БД)
- Gradle (Groovy DSL)
- Lombok
- MapStruct
- Swagger/OpenAPI
- Docker + docker-compose

## Требования

- Java 21
- Docker и docker-compose (для контейнеризации)
- PostgreSQL 15 (при локальном запуске)

## Запуск с помощью Docker

1. Клонировать репозиторий
2. Скопировать `.env.example` в `.env` и настроить переменные окружения
3. Запустить контейнеры:
```bash
docker-compose up -d