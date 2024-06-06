FROM maven:3.8.3-amazoncorretto-17

# Set the working directory in the container
WORKDIR /app
# Copy the pom.xml and the project files to the container
COPY pom.xml .
# Cache dependencies in docker in case we're just updating src, we won't need to download dependencies again
RUN mvn dependency:resolve
# Copy the source files
COPY src ./src
# Build the application using Maven
RUN mvn clean package -DskipTests

# Run the packaged JAR file
WORKDIR /app/target
ENTRYPOINT ["java", "-Xmx2048M", "-jar", "bank-ledger-api-0.0.1-SNAPSHOT.jar"]
