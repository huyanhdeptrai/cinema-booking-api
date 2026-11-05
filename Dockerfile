# build ứng dụng bằng maven và jdk 21
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# tải thư viện trước để tận dụng cache khi chỉ sửa code
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src

# test được chạy riêng trước khi build image
RUN mvn clean package -DskipTests

# image chạy ứng dụng chỉ cần jre
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# tạo user riêng để chạy ứng dụng
RUN addgroup -S appgroup \
    && adduser -S appuser -G appgroup

COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]