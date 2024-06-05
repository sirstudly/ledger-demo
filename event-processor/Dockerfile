#
# Build stage
#
FROM amazoncorretto:17 AS build-ep

# Set the working directory in the container
WORKDIR /app
# Copy the pom.xml and the project files to the container
COPY pom.xml mvnw* .
COPY .mvn ./.mvn
# cache dependencies in docker in case we're just updating src, we won't need to download dependencies again
RUN  yum -y install tar gzip
RUN  ./mvnw dependency:resolve
COPY src ./src

# Build the application using Maven
RUN ./mvnw clean package -DskipTests

#
# Create and run the container
#
FROM amazoncorretto:17

# Copy the built JAR file from the previous stage to the container
COPY --from=build-ep /app/target/event-processor-0.0.1-SNAPSHOT.jar ./

#ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:4004

ENTRYPOINT ["java", "-Xmx2048M", "-jar", "event-processor-0.0.1-SNAPSHOT.jar"]
