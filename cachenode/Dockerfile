FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY . .

RUN mvn -pl cachenode -am clean package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/cachenode/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
