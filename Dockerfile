# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copier les fichiers Maven wrapper depuis annuaire_back/
COPY annuaire_back/.mvn/ .mvn
COPY annuaire_back/mvnw annuaire_back/pom.xml ./

RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline

# Copier le code source depuis annuaire_back/
COPY annuaire_back/src ./src

RUN ./mvnw clean package -DskipTests

# Stage 2: Production
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Installer SSH client, netcat et bash pour le tunnel
RUN apk add --no-cache curl openssh-client netcat-openbsd bash

# Créer un utilisateur non-root
RUN addgroup -S spring && adduser -S spring -G spring

# Copier le JAR et le script d'entrypoint
COPY --from=build /app/target/*.jar app.jar
COPY annuaire_back/docker-entrypoint.sh /app/docker-entrypoint.sh

# Rendre le script exécutable et changer les permissions
RUN chmod +x /app/docker-entrypoint.sh && \
    chown spring:spring /app/docker-entrypoint.sh /app/app.jar

USER spring:spring

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["/app/docker-entrypoint.sh"]
