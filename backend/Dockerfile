# ==========================================
# Stage 1: Build & Package
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Install dos2unix to ensure shell scripts run cleanly across Windows/Linux hosts
RUN apk add --no-cache dos2unix

# Copy Maven wrapper and descriptor to leverage Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN dos2unix mvnw && chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy application source tree and package executable fat JAR
COPY src/ src/
RUN ./mvnw clean package -DskipTests

# ==========================================
# Stage 2: Minimal Production Runtime
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Run as non-root user for container security principles
RUN addgroup -S soragroup && adduser -S sorauser -G soragroup
USER sorauser

# Copy compiled JAR from Stage 1
COPY --from=builder /build/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=docker
EXPOSE 8081

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]