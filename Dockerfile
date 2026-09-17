# Build the Spring Boot application
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw.cmd ./
RUN chmod +x mvnw
RUN ./mvnw -B dependency:go-offline
COPY src src
RUN ./mvnw -B -DskipTests package

# Run with a small Java runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends fontconfig fonts-liberation \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
