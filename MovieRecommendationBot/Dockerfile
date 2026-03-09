FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Копируем JAR из target/
COPY target/MovieRecommendationBot-*.jar app.jar

EXPOSE 8110

ENTRYPOINT ["java", "-jar", "app.jar"]