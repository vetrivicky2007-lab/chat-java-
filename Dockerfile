FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN mvn clean package

EXPOSE 10000

CMD ["java", "-jar", "target/unihive-1.0.0.jar"]