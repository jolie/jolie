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

# Download and use the release tools to generate jolie_installer.jar
WORKDIR /
RUN git clone --depth=1 https://github.com/jolie/release_tools.git
WORKDIR /release_tools/jolie_installer
RUN mvn -T 1C -Dmaven.test.skip -DskipTests clean install
WORKDIR /release_tools
RUN apk add zip
RUN jolie release.ol ../jolie-git

# Start from scratch, copy the installer, install, remove the installer.
FROM adoptopenjdk/openjdk11:alpine
WORKDIR /
COPY --from=JolieBuild /release_tools/release/jolie_installer.jar .
RUN apk add --update zip \
    && java -jar jolie_installer.jar -jh /usr/lib/jolie -jl /usr/bin && rm jolie_installer.jar
ENV JOLIE_HOME /usr/lib/jolie
