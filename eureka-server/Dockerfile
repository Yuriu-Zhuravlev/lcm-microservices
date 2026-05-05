FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

RUN apk add --no-cache maven

COPY pom.xml .
COPY common-dto/pom.xml common-dto/
COPY course-service/pom.xml course-service/
COPY auth-service/pom.xml auth-service/
COPY api-gateway/pom.xml api-gateway/
COPY eureka-server/pom.xml eureka-server/
COPY learning-service/pom.xml learning-service/

RUN mvn dependency:go-offline -B

COPY eureka-server/src eureka-server/src

RUN mvn clean package -DskipTests -pl eureka-server -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/eureka-server/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]