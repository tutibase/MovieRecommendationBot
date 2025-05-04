# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем pom.xml и зависимости
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем исходный код
COPY src ./src

# Генерация кода JOOQ
RUN mvn generate-sources

# Собираем проект
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем собранный JAR из первого этапа
COPY --from=build /app/target/MovieRecommendationBot-1.0-SNAPSHOT.jar /app/app.jar
COPY --from=build /app/src/main/resources/bot.properties /app/bot.properties

# Команда для запуска приложения
ENTRYPOINT ["java", "-jar", "app.jar"]