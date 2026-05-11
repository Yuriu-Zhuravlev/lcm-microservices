FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

RUN apk add --no-cache maven

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

RUN java -version && mvn -version

COPY pom.xml .
COPY common-dto/pom.xml common-dto/
COPY course-service/pom.xml course-service/
COPY auth-service/pom.xml auth-service/
COPY api-gateway/pom.xml api-gateway/
COPY eureka-server/pom.xml eureka-server/
COPY learning-service/pom.xml learning-service/

RUN mvn dependency:go-offline -B

COPY common-dto/src common-dto/src
COPY learning-service/src learning-service/src

RUN mvn clean install -DskipTests -pl common-dto -am
RUN mvn clean package -pl learning-service -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/learning-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]