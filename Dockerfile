# Use a lightweight OpenJDK image
FROM eclipse-temurin:25-jdk

# Set the working directory
WORKDIR /app

# Copy the built jar file
COPY build/libs/app.jar app.jar

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

