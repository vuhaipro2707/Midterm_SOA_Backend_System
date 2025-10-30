FROM maven:3.9.8-eclipse-temurin-21 as builder

WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn clean install -DskipTests


FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache tzdata
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]