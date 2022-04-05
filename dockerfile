FROM openjdk:11 AS base
RUN apt-get update && apt-get install -y maven

FROM base AS development
RUN useradd -s /bin/bash -m dev \
    && groupadd docker \
    && usermod -aG docker dev
RUN apt-get install -y git
ENV LIB ./target
USER dev

FROM base AS build
WORKDIR /logpoints
COPY . .
RUN mvn clean package

FROM openjdk:11
WORKDIR /logpoints
COPY --from=build /logpoints/target/logpoints-*.jar lib/
COPY --from=build /logpoints/target/dependency/ lib/
COPY --from=build /logpoints/logpoints bin/
ENV PATH="/logpoints/bin:${PATH}"
ENV LOGPOINTS_HOME="/logpoints"
ENTRYPOINT logpoints



