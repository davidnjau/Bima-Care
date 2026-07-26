FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/*-all.jar app.jar
EXPOSE 8093
ENTRYPOINT ["java", "-jar", "app.jar"]
