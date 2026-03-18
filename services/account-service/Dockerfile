FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml pom.xml
COPY services/account-service/pom.xml services/account-service/pom.xml
COPY services/account-service/src services/account-service/src

RUN mvn -f services/account-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/services/account-service/target/account-service-0.1.0-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
