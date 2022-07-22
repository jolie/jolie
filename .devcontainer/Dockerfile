FROM maven:3-eclipse-temurin-17

# Update apt db
RUN apt-get update

# Install sudo
RUN apt-get install -y sudo

# Set JOLIE_HOME
ENV JOLIE_HOME=/usr/bin/jolie-dist

# Add the jolie user and make it a sudoer
RUN useradd -m jolie -s /bin/bash \
	&& echo 'jolie ALL=(ALL:ALL) NOPASSWD: ALL' >> /etc/sudoers.d/jolie \
	&& chmod 0440 /etc/sudoers.d/jolie

USER jolie