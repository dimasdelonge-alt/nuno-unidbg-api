# Stage 1: Build the Fat JAR using Maven and OpenJDK 11
FROM maven:3.8.4-openjdk-11-slim AS build

# Clone and compile Unidbg locally to bypass Jitpack POM failure
RUN apt-get update && apt-get install -y git
RUN git clone https://github.com/zhkl0228/unidbg.git /unidbg
WORKDIR /unidbg
RUN git checkout v0.9.8
RUN mvn clean install -DskipTests -Dmaven.javadoc.skip=true -Dmaven.test.skip=true -Dgpg.skip=true

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the actual source files and native libraries
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Create the minimal runner image
FROM openjdk:11-jre-slim
WORKDIR /app

# Copy the packaged fat-jar from the build stage
COPY --from=build /app/target/nuno-unidbg-server-1.0-SNAPSHOT-jar-with-dependencies.jar ./app.jar

# Hugging Face Spaces default port (internal container)
EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
