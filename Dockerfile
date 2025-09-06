# ---------- Сборка ----------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Копируем wrapper и зависимости, чтобы слои кешировались
COPY mvnw* ./
COPY .mvn .mvn
COPY pom.xml ./
RUN ./mvnw dependency:go-offline -B

# Копируем исходники и собираем jar
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ---------- Запуск ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

# Копируем собранный jar
COPY --from=build /app/target/*.jar app.jar

# Открываем порт
EXPOSE 8080

# Запуск приложения
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
