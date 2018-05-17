FROM arm32v7/openjdk as JolieBuild

# Download and install Jolie. We need it for running the release tools.
RUN apt-get update
RUN apt-get -y install ant
RUN git clone https://github.com/jolie/jolie.git jolie-git
WORKDIR /jolie-git
RUN ant dist-install
ENV JOLIE_HOME /usr/lib/jolie

# Download and use the release tools to generate jolie_installer.jar
WORKDIR /
RUN git clone https://github.com/jolie/release_tools.git
WORKDIR /release_tools/jolie_installer
RUN ant jar
WORKDIR /release_tools
RUN apt-get -y install zip
RUN jolie release.ol ../jolie-git

# Start from scratch, copy the installer, install, remove the installer.
FROM arm32v7/openjdk
WORKDIR /
COPY --from=JolieBuild /release_tools/release/jolie_installer.jar .
RUN java -jar jolie_installer.jar -jh /usr/lib/jolie -jl /usr/bin
RUN rm jolie_installer.jar
ENV JOLIE_HOME /usr/lib/jolie
