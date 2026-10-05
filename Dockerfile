FROM gradle:8-jdk17 AS build

WORKDIR /workspace

# Copiar common-events
COPY common-events /workspace/common-events

# Publicar common-events en Maven Local
WORKDIR /workspace/common-events
RUN chmod +x gradlew
RUN ./gradlew publishToMavenLocal --no-daemon

# Copiar product-service
COPY product-service /workspace/product-service

# Compilar product-service
WORKDIR /workspace/product-service
RUN chmod +x gradlew
RUN ./gradlew clean bootJar --no-daemon


# Imagen final
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /workspace/product-service/build/libs/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]