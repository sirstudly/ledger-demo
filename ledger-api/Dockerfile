#
# Build stage
#
FROM amazoncorretto:17 AS build

# Set the working directory in the container
WORKDIR /app
# Copy the pom.xml and the project files to the container
COPY pom.xml mvnw* .
COPY .mvn ./.mvn
# cache dependencies in docker in case we're just updating src, we won't need to download dependencies again
RUN  ./mvnw dependency:resolve
COPY src ./src

# Build the application using Maven
RUN ./mvnw clean package -DskipTests

#
# Create and run the container
#
FROM amazoncorretto:17

# Use an official OpenJDK image as the base image
#FROM openjdk:11-jre-slim
# Set the working directory in the container
#WORKDIR /app
# Copy the built JAR file from the previous stage to the container
COPY --from=build /app/target/bank-ledger-api-0.0.1-SNAPSHOT.jar ./
# Set the command to run the application
#CMD ["java", "-jar", "application.jar"]

#EXPOSE 4000
#ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005

#ARG JAR_FILE=target/*.jar
#COPY ${JAR_FILE} application.jar
#CMD apt-get update -y
ENTRYPOINT ["java", "-Xmx2048M", "-jar", "bank-ledger-api-0.0.1-SNAPSHOT.jar"]
