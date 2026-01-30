# Spring Boot应用Dockerfile

FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /workspace/app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN chmod +x ./gradlew
RUN ./gradlew build -x test

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 安装字体（支持中日韩文本渲染）
RUN apk add --no-cache fontconfig ttf-dejavu

COPY --from=build /workspace/app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
