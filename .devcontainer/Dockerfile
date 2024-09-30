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


FROM maven:3-eclipse-temurin-21

# Get rid of the ubuntu user
RUN userdel ubuntu

# Update apt db
RUN apt-get update

# Install sudo
RUN apt-get install -y sudo

# Set JOLIE_HOME
ENV JOLIE_HOME=/usr/bin/jolie-dist


# Add the jolie user and make it a sudoer
ARG USERNAME=jolie
ARG USER_UID=1000
# ARG USER_GID=$USER_UID

# RUN useradd --uid $USER_UID --gid $USER_GID -m $USERNAME -s /bin/bash \
RUN useradd --uid $USER_UID -m $USERNAME -s /bin/bash \
	&& echo "$USERNAME ALL=(ALL:ALL) NOPASSWD: ALL" >> /etc/sudoers.d/jolie \
	&& chmod 0440 /etc/sudoers.d/jolie

USER $USERNAME