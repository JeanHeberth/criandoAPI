FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY src src

RUN chmod +x gradlew && ./gradlew bootWar --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /app/build/libs/*.war app.war

ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8089

ENTRYPOINT ["java", "-jar", "app.war"]
