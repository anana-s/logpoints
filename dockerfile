FROM openjdk:11 AS base
RUN apt-get update && apt-get install -y maven

FROM base AS development
RUN apt-get install -y git docker sudo curl vim
RUN groupadd docker && useradd -m -s /bin/bash -G docker,sudo dev
RUN echo '%sudo ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers
USER dev
ENV LIB ./target

FROM base AS build
WORKDIR /logpoints
COPY . .
RUN mvn clean package && mkdir lib && cp target/logpoints-*.jar target/dependency/*.jar lib/

FROM openjdk:11
WORKDIR /logpoints
COPY --from=build /logpoints/lib/ lib/
COPY --from=build /logpoints/logpoints bin/
ENV PATH="/logpoints/bin:${PATH}"
ENV LOGPOINTS_HOME="/logpoints"
ARG mx="4G"
ENV LOGPOINTS_JVM_OPTS="-Xmx${mx}"
EXPOSE 7000
ENTRYPOINT ["logpoints", "serve"]
