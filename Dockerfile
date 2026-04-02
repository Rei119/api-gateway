FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests
EXPOSE 10000
CMD ["java", "-jar", "target/api-gateway-0.0.1-SNAPSHOT.jar"]