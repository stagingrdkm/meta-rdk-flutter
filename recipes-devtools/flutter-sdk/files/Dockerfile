#
# Copyright (C) 2018-2020 Damian Wrobel <dwrobel@ertelnet.rybnik.pl>
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#
ARG BASE_IMAGE=fedora:36
FROM $BASE_IMAGE

ARG SDK_RELEASE

LABEL maintainer="dwrobel@ertelnet.rybnik.pl" description="Podman/Docker image for cross compiling flutter apps for CPE"

RUN dnf install -y ccache git-core sudo

RUN echo >/etc/sudoers.d/wheel-no-passwd '%wheel	ALL=(ALL)	NOPASSWD: ALL'

RUN dnf update -y

RUN dnf install -y binutils rsync buildah which file xz cpio unzip cmake ninja-build clang pkgconfig\(gtk+-3.0\) weston mesa-dri-drivers xorg-x11-server-Xvfb findutils python3-xlrd xz-devel libcanberra-gtk3 sqlite jq

ADD flutter-bundler /sdk/flutter/bin/

ENV PATH="/sdk/flutter/bin/:${PATH}"
COPY sdk /sdk
RUN chmod -R a+rwx /sdk
#there shall be used 4.15+ kernel version for ubuntu 16.04LTS to do not observe permissions issue


RUN  mkdir -p /flutter-wayland-app
RUN chmod a+rwx /flutter-wayland-app
COPY files /flutter-wayland-app/files
RUN chmod -R a+w flutter-wayland-app/files

RUN echo $SDK_RELEASE >/sdk-release
RUN mkdir -p /mnt/cache
RUN chmod -R a+rwx /mnt

RUN dnf clean all
ADD entrypoint.sh /
