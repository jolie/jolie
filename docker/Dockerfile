# Copyright (C) 2022 Fabrizio Montesi <famontesi@gmail.com>
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
# MA 02110-1301 USA

# To use this dockerfile locally, launch the following command from the root directory of your local copy of this repository.
# docker build -f docker/Dockerfile -t jolielang/jolie .

FROM maven:3-eclipse-temurin-21 as JolieBuild

# Compile and install Jolie. We need it for running the release tools.
COPY . /jolie-git
WORKDIR /jolie-git
RUN mvn -T 1C -Dmaven.test.skip -DskipTests -Dpmd.skip=true -pl '!test' clean install
RUN cp -avr /jolie-git/dist/jolie/ /usr/lib/jolie/
RUN cp -a /jolie-git/dist/launchers/unix/.  /usr/bin/
ENV JOLIE_HOME /usr/lib/jolie

# Compile the Jolie installer
WORKDIR /jolie-git/release-tools/installer
RUN mvn -T 1C -Dmaven.test.skip -DskipTests -Dpmd.skip=true clean install

# Use the release tools to generate jolie-installer.jar
RUN apt-get update && apt-get install -y zip
WORKDIR /jolie-git/release-tools
RUN jolie release.ol ..

# Start from scratch, copy the installer, install, remove the installer.
FROM eclipse-temurin:21-jre
WORKDIR /
COPY --from=JolieBuild /jolie-git/release-tools/release/jolie-installer.jar .
RUN apt-get update && apt-get install -y unzip \
    && java -jar jolie-installer.jar -jh /usr/lib/jolie -jl /usr/bin \
    && rm jolie-installer.jar && rm -rf /tmp/*
ENV JOLIE_HOME /usr/lib/jolie
