FROM eclipse-temurin:25-jdk AS Builder

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q

COPY src ./src
RUN ./mvnw package -DskipTests -q

RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted
