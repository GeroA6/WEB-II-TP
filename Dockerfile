# Etapa 1: Build (Compilación y empaquetado del JAR)
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Copiamos los archivos del wrapper de Maven y el pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Damos permisos de ejecución al wrapper y descargamos dependencias
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

#Copiamos el código fuente y empaquetamos el JAR (omitiendo tests)
COPY src ./src
RUN ./mvnw clean package -DskipTests -B


# Etapa 2: Runtime (Imagen final liviana para ejecución)
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

#Buena práctica de seguridad: no correr la aplicación como root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiamos únicamente el .jar generado en la etapa anterior
COPY --from=builder /app/target/*.jar app.jar

#Documentamos el puerto donde escucha Spring Boot
EXPOSE 8080

# Comando para iniciar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]