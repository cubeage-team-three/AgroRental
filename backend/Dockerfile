# =============================================================================
# Stage 1: Build Application with Maven and Temurin JDK 21
# =============================================================================
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app

# Copy Maven POM and project sources
COPY pom.xml .
COPY src ./src

# Package production executable JAR without running tests during image build
RUN mvn clean package -DskipTests -B

# =============================================================================
# Stage 2: Minimal, Secure Production Runtime with Temurin JRE 21
# =============================================================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create unprivileged application user and group for container security
RUN groupadd -r agrorent && useradd -r -g agrorent agrorent

# Initialize upload directories with proper non-root permissions
RUN mkdir -p /app/uploads/equipment /app/uploads/avatars \
    && chown -R agrorent:agrorent /app

# Copy generated executable fat JAR from the builder stage
COPY --from=build --chown=agrorent:agrorent /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

# Switch to unprivileged runtime user
USER agrorent

# Default port configuration (Render overrides this dynamically via $PORT)
ENV PORT=8080
EXPOSE ${PORT}

# JVM entrypoint respecting container RAM limits (512MB limit on Render Free Tier)
# and dynamically binding to the assigned Render $PORT
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:--XX:MaxRAMPercentage=75.0 -Xmx384m} -Dserver.port=${PORT} -jar app.jar"]
