# Stage 1: Build the Fat JAR using Maven and OpenJDK 11
FROM maven:3.8.4-openjdk-11-slim AS build
WORKDIR /app

# We define this argument to allow passing a GitHub personal access token if jitpack is temperamental, though usually public repos are fine.
COPY pom.xml .
# Download all dependencies first to cache this layer
RUN mvn dependency:go-offline -B

# Copy the actual source files and native libraries
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Create the minimal runner image
FROM openjdk:11-jre-slim
WORKDIR /app

# Copy the packaged fat-jar from the build stage
COPY --from=build /app/target/nuno-unidbg-server-1.0-SNAPSHOT-jar-with-dependencies.jar ./app.jar

# Render exposes PORT environment variable
EXPOSE 8080

# Javalin looks at PORT environment variable naturally if we pass it or we just use it 
CMD ["java", "-jar", "app.jar"]
