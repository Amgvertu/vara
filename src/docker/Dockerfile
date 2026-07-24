# Этап сборки
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Копирование файлов Gradle
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY gradle.properties .

# Копирование исходного кода
COPY src src

# Сборка приложения
RUN chmod +x gradlew
RUN ./gradlew bootJar -x test

# Этап выполнения
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Создание непривилегированного пользователя
RUN addgroup -S varamy && adduser -S varamy -G varamy

# Копирование собранного JAR-файла
COPY --from=builder /app/build/libs/varamy.jar app.jar

# Создание директории для логов и загрузок
RUN mkdir -p /var/log/varamy /app/uploads && \
    chown -R varamy:varamy /var/log/varamy /app/uploads

# Переключение на непривилегированного пользователя
USER varamy

# Порт приложения
EXPOSE 8081

# Запуск приложения
ENTRYPOINT ["java", "-jar", "app.jar"]