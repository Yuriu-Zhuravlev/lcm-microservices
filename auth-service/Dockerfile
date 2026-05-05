FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Встановлюємо Maven вручну (це швидко в Alpine)
RUN apk add --no-cache maven

# Копіюємо структуру проектів
COPY pom.xml .
COPY common-dto/pom.xml common-dto/
COPY course-service/pom.xml course-service/
COPY auth-service/pom.xml auth-service/
COPY api-gateway/pom.xml api-gateway/
COPY eureka-server/pom.xml eureka-server/
COPY learning-service/pom.xml learning-service/

# Завантажуємо залежності
RUN mvn dependency:go-offline -B

# Копіюємо код
COPY common-dto/src common-dto/src
COPY auth-service/src auth-service/src

# Збираємо модулі
RUN mvn clean install -DskipTests -pl common-dto -am
RUN mvn clean package -DskipTests -pl auth-service -am

# 2. Етап запуску
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/auth-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]