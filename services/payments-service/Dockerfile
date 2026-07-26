FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/*-all.jar app.jar
EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar"]
