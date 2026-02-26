# Stage 1: Build the JAR using Gradle
FROM gradle:8.7-jdk17 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
RUN gradle build -x test --no-daemon

# Stage 2: Run the JAR using Java
FROM openjdk:17-jdk-slim
WORKDIR /app
# This line grabs the built jar from the first stage
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]