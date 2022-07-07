#
# To use this dockerfile locally, launch the following command from the root directory of your local copy of this repository.
# docker build -f docker/Dockerfile -t jolielang/jolie .

FROM adoptopenjdk/openjdk11:alpine as JolieBuild

# Download and install Jolie. We need it for running the release tools.
RUN apk add --update git maven
COPY . /jolie-git
WORKDIR /jolie-git
RUN mvn -T 1C -Dmaven.test.skip -DskipTests -pl '!test' clean install
RUN cp -avr /jolie-git/dist/jolie/ /usr/lib/jolie/
RUN cp -a /jolie-git/dist/launchers/unix/.  /usr/bin/
ENV JOLIE_HOME /usr/lib/jolie

# Compile the Jolie installer
WORKDIR /jolie-git/release-tools/installer
RUN mvn -T 1C -Dmaven.test.skip -DskipTests clean install

# Use the release tools to generate jolie-installer.jar
RUN apk add zip
WORKDIR /jolie-git/release-tools
RUN jolie release.ol ..

# Start from scratch, copy the installer, install, remove the installer.
FROM adoptopenjdk/openjdk11:alpine
WORKDIR /
COPY --from=JolieBuild /jolie-git/release-tools/release/jolie-installer.jar .
RUN apk add --update zip \
    && java -jar jolie-installer.jar -jh /usr/lib/jolie -jl /usr/bin \
    && rm jolie-installer.jar
ENV JOLIE_HOME /usr/lib/jolie
