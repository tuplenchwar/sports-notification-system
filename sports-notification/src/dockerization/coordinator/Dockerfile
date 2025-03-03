# Use official OpenJDK base image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Set environment variables
ENV AWS_REGION=us-west-2
ENV AWS_ACCESS_KEY_ID=XXXXXX
ENV AWS_SECRET_ACCESS_KEY=XXXX

# Update package list and install curl
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy JAR file into the container
COPY out/artifacts/sports_notification_jar/sports-notification.jar /app/sports-notification.jar

# Expose the necessary port (Change if needed)
EXPOSE 8080

# Command to run the Coordinator service
CMD ["java", "-jar", "/app/sports-notification.jar", "--spring.profiles.active=coordinator"]
