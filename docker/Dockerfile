FROM openjdk:slim as JolieBuild

RUN apt-get update
RUN apt-get -y install ant
RUN apt-get -y install git
RUN git clone --depth=1 https://github.com/jolie/jolie.git --branch next jolie-git
WORKDIR /jolie-git
RUN ant dist-install
ENV JOLIE_HOME /usr/local/lib/jolie
WORKDIR /home
