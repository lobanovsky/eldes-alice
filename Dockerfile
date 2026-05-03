FROM gradle:8.12-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle --no-daemon installDist

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/install/eldes-alice /app
ENV PORT=8081
EXPOSE 8081
CMD ["/app/bin/eldes-alice"]
