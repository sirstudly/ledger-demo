FROM maven:3.8.3-amazoncorretto-17

# Set the working directory in the container
WORKDIR /commons-build
# Copy the pom.xml and the project files to the container
COPY pom.xml .
# Cache dependencies in docker in case we're just updating src, we won't need to download dependencies again
RUN mvn dependency:resolve
# Copy the source files
COPY src ./src
# Build the application using Maven
RUN mvn clean install

ENTRYPOINT ["echo", "Required only for build process. Exiting now."]
