#
# If not stated otherwise in this file or this component's LICENSE file the
# following copyright and licenses apply:
#
# Copyright 2019-2020 Liberty Global B.V.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Author: Damian Wrobel <dwrobel@ertelnet.rybnik.pl>
#

require recipes-devtools/flutter/flutter-common.inc

SUMMARY     = "Flutter Wayland embedding launcher"
HOMEPAGE    = "https://github.com/dwrobel/flutter_wayland"

LICENSE = "BSD"
SRC_URI  = "git://github.com/LibertyGlobal/flutter-embedder-wayland.git;protocol=https;branch=${FLUTTER_LAUNCHER_WAYLAND_SRCBRANCH};rev=${FLUTTER_LAUNCHER_WAYLAND_SRCREV}"

TOOLCHAIN = "clang"

FILESEXTRAPATHS_prepend := "${THISDIR}/files:"

DEPENDS = "flutter-engine gtk+3 libxkbcommon wayland-native wayland-protocols extra-cmake-modules"

LIC_FILES_CHKSUM = "file://LICENSE;md5=5c812f8f3c95dc6811ef68cb1eef87e5"

S = "${WORKDIR}/git"

inherit pkgconfig cmake

FILES_${PN} = "${bindir}"
