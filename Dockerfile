FROM maven:3.9.9-eclipse-temurin-21

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests

EXPOSE 10000

CMD ["java", "-jar", "target/unihive-1.0.0.jar"]