FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy source files and standalone POM
COPY src ./src
COPY pom-standalone.xml ./pom.xml

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the JAR from build stage
COPY --from=build /app/target/user-service-1.0.0.jar app.jar

# Create log directory
RUN mkdir -p /var/log

# Run the application
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
