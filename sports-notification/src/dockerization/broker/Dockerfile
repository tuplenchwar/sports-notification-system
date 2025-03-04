# Use official OpenJDK base image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Update package list and install curl
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy JAR file into the container
COPY out/artifacts/sports_notification_jar/sports-notification.jar /app/sports-notification.jar

# Expose the necessary port
EXPOSE 8080

# Run application and fetch EC2 Public IP at runtime
CMD ["sh", "-c", "BROKER_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4) && \
  java -jar /app/sports-notification.jar \
  --spring.profiles.active=broker \
  --coordinator.url=http://coordinator-sports-notification.click:8080 \
  --broker.url=http://$BROKER_IP \
  --server.port=8080"]
