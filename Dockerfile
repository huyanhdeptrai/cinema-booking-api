# 1. build mã nguồn ra file .jar
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# copy pom.xml và tải trước các thư viện (để tối ưu cache của Docker)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# copy toàn bộ mã nguồn vào và tiến hành đóng gói (bỏ qua chạy test khi build image để nhanh hơn)
COPY src ./src
RUN mvn clean package -DskipTests

# 2. tạo image siêu nhẹ để chạy ứng dụng
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# copy file .jar vừa được build xong từ Giai đoạn 1 sang
COPY --from=build /app/target/*.jar app.jar

# mở cổng 8080 cho container
EXPOSE 8080

# câu lệnh khởi chạy ứng dụng khi bật container
ENTRYPOINT ["java", "-jar", "app.jar"]