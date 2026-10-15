FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY . .
RUN mvn -B -ntp -Dmaven.test.skip=true package

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /workspace/discovery-service/target/discovery-service-1.0.0-SNAPSHOT.jar /app/discovery-service.jar
COPY --from=build /workspace/api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar /app/api-gateway.jar
COPY --from=build /workspace/station-service/target/station-service-1.0.0-SNAPSHOT.jar /app/station-service.jar
COPY --from=build /workspace/train-service/target/train-service-1.0.0-SNAPSHOT.jar /app/train-service.jar
COPY --from=build /workspace/route-service/target/route-service-1.0.0-SNAPSHOT.jar /app/route-service.jar
COPY --from=build /workspace/booking-service/target/booking-service-1.0.0-SNAPSHOT.jar /app/booking-service.jar
COPY --from=build /workspace/payment-service/target/payment-service-1.0.0-SNAPSHOT.jar /app/payment-service.jar
COPY --from=build /workspace/order-service/target/order-service-1.0.0-SNAPSHOT.jar /app/order-service.jar

RUN useradd --system --uid 10001 appuser
USER 10001

ENTRYPOINT ["sh", "-c", "exec java -jar /app/${SERVICE_JAR}"]