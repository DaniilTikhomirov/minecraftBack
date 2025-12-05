# ---------- Сборка ----------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY mvnw* ./
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests

# ---------- Запуск ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

# Добавляем volume для storage
VOLUME ["/app/storage"]

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
