# Stage 1: Build the application using Maven
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Give execution permission to mvnw (Fixes permission denied error)
RUN chmod +x mvnw

# Copy source code
COPY src src

# Build the jar file (skip tests to speed up deployment)
RUN ./mvnw clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copy the built jar file from the build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

ENTRYPOINT ["java", "-Djava.awt.headless=true", "-XX:MaxRAMPercentage=70", "-XX:+UseSerialGC", "-jar", "app.jar"]