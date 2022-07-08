#!/bin/sh

docker run -it --rm --name jolie-build --user $(id -u):$(id -g) \
    -v "$HOME/.m2":/var/maven/.m2 -e MAVEN_CONFIG=/var/maven/.m2 \
    -v "$(pwd)/..":/usr/src/mymaven -w /usr/src/mymaven maven:3-jdk-11 \
    mvn -T 1C clean install -Duser.home=/var/maven -Dmaven.test.skip -DskipTests -pl '!test'

docker run -it --rm --name jolie-installer-build --user $(id -u):$(id -g) \
    -v "$HOME/.m2":/var/maven/.m2 -e MAVEN_CONFIG=/var/maven/.m2 \
    -v "$(pwd)/installer":/usr/src/mymaven -w /usr/src/mymaven maven:3-jdk-11 \
    mvn -T 1C clean install -Duser.home=/var/maven
