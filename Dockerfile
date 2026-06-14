FROM bellsoft/liberica-openjdk-alpine:21 AS builder

WORKDIR /app

# 의존성 캐시 레이어 분리 (build.gradle 변경 시에만 재다운로드)
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon -q || true

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test


# Run stage

FROM bellsoft/liberica-openjdk-alpine:21

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]