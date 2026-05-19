
# Use lightweight Java runtime
FROM eclipse-temurin:21-jre

# Copy your built jar into container
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# Run the app
ENTRYPOINT ["java","-jar","/app.jar"]
