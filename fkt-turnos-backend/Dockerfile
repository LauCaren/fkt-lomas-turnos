# Build multi-stage: compila con el JDK, corre con un JRE liviano.
# Pensado para Railway/Render/Fly.io (build por Dockerfile, sin cambios de código:
# toda la config sensible ya sale de variables de entorno, ver README.md).

FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Cachear las dependencias en su propia capa antes de copiar el código fuente
# (dependency:go-offline no siempre resuelve el árbol completo — ej. dependencias
# de test — así que el package final no fuerza --offline, solo se beneficia del caché).
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
