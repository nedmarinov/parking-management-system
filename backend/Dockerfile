FROM eclipse-temurin:21.0.12_8-jdk-jammy AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
# Cache dependencies in their own layer so source changes do not re-download them.
RUN ./mvnw --batch-mode --no-transfer-progress dependency:go-offline
COPY src/ src/
RUN ./mvnw --batch-mode --no-transfer-progress package

FROM eclipse-temurin:21.0.12_8-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --user-group parking

COPY --from=build --chown=parking:parking /app/target/parking-0.0.1-SNAPSHOT.jar app.jar
USER parking
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
