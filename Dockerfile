# Stage 1: Build
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copy only Gradle files first (maximizes cache)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Cache Gradle downloads
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies

# Now copy source
COPY src src

# Build the jar (reuse cache)
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar

# Stage 2: Runtime
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

#
## Stage 1: Build the application
#FROM eclipse-temurin:17-jdk AS build
#WORKDIR /app
## Copy the Gradle wrapper and project files
#COPY gradlew .
#COPY gradle gradle
#COPY build.gradle build.gradle
#COPY settings.gradle settings.gradle
#COPY src src
## Build the JAR file
#RUN ./gradlew bootJar
#
## Stage 2: Run the application
#FROM eclipse-temurin:17-jre-alpine
#WORKDIR /app
## Copy the JAR file from the build stage
#COPY --from=build /app/build/libs/*.jar app.jar
## Command to run the application
#ENTRYPOINT ["java", "-jar", "app.jar"]
