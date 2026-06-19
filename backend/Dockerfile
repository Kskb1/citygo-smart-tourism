FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV JAVA_OPTS=""
RUN apt-get update \
  && apt-get install -y --no-install-recommends wget \
  && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar /app/citygo-backend.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=5 \
  CMD ["sh", "-c", "wget -qO- http://localhost:8080/api/health | grep -q '\"status\"'"]
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/citygo-backend.jar"]
