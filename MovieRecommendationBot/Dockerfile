# Используем легкий образ с Java
FROM eclipse-temurin:21-jre

# Рабочая директория
WORKDIR /app

# Копируем уже собранный JAR из контекста сборки (его положит туда Jenkins/pipeline)
# Важно: имя файла должно совпадать с тем, что лежит в target/
COPY target/MovieRecommendationBot-1.0-SNAPSHOT.jar app.jar

# Запуск
ENTRYPOINT ["java", "-jar", "app.jar"]