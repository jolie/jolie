FROM openjdk:alpine as JolieBuild

RUN apk update
RUN apk add apache-ant
RUN apk add git
RUN git clone https://github.com/saltgz/jolie.git jolie-git
WORKDIR /jolie-git
RUN ant && ant install
ENV JOLIE_HOME /usr/local/lib/jolie
WORKDIR /home
