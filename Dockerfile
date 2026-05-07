# --- Build stage ---
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Cache deps first
COPY gradle gradle
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

# Build
COPY src src
RUN ./gradlew --no-daemon installDist

# --- Runtime stage ---
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/install/hackathon ./

ENV PORT=8080
EXPOSE 8080

CMD ["./bin/hackathon"]
