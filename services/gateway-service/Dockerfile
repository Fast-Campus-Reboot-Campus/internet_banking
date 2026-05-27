FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY services/gateway-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
