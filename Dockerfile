
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /app/target/demobank.jar app.jar
COPY config.properties .
COPY web ./web
COPY data ./data

EXPOSE 10000

CMD ["sh", "-c", "java -jar app.jar"]
