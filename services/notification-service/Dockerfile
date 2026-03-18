FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml pom.xml
COPY services/notification-service/pom.xml services/notification-service/pom.xml
COPY services/notification-service/src services/notification-service/src

RUN mvn -f services/notification-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/services/notification-service/target/notification-service-0.1.0-SNAPSHOT.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
