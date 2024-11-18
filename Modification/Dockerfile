# Use an official Gradle image as a base
FROM gradle:jdk17 AS builder

# Set the working directory in the container
WORKDIR /app

# Copy essential Gradle files to the container
COPY build.gradle settings.gradle /app/
COPY gradlew gradle/wrapper /app/

# Ensure gradlew has execute permissions
RUN chmod +x /app/gradlew

# Debug: Show files copied so far
RUN find . -type f | sort

# Copy the rest of the project files
COPY . /app/

# Force Gradle to use a fresh build and provide detailed output
RUN ./gradlew clean --no-build-cache build --info

# Use a smaller image for the final container
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy only the JARs (compiled application) to the final image
COPY --from=builder /app/build/libs /app/libs

# Debug: Verify JARs copied
RUN ls -l /app/libs

# Run the application
CMD ["java", "-jar", "/app/libs/Modification.jar"]
