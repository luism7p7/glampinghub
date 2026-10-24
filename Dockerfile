# --- Etapa 1: compilar con Maven ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY CampingHub/pom.xml .
RUN mvn -q -B dependency:go-offline
COPY CampingHub/src ./src
RUN mvn -q -B package -DskipTests

# --- Etapa 2: imagen liviana solo con el JRE ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Ajustado para el plan gratis de Render (512 MB de RAM)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"
EXPOSE 8080
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
