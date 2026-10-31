FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml .

RUN mvn -B -ntp dependency:go-offline

COPY src ./src

RUN mvn -B -ntp -DskipTests package \
    && cp target/fuelfinder-*.jar app.jar

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /build/app.jar app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8"

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
