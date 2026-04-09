FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Копируем собранный JAR
COPY build/libs/*.jar app.jar

# Порт приложения
EXPOSE 8110

# Запуск
ENTRYPOINT ["java", "-jar", "app.jar"]