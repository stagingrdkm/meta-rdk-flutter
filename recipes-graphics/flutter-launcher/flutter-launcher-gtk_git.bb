#
# If not stated otherwise in this file or this component's LICENSE file the
# following copyright and licenses apply:
#
# Copyright 2020 Liberty Global B.V.
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

SUMMARY     = "Flutter gtk based launcher for embedded platforms."
HOMEPAGE    = "https://github.com/dwrobel/flutter-gtk-embedding"

LICENSE = "Apache-2.0"

SRC_URI  = "git://github.com/LibertyGlobal/flutter-gtk-embedding;protocol=https;rev=2d7fac802ba5abe3e1537160786a8ca5c9b9a60d"

require recipes-devtools/flutter/flutter-common.inc

TOOLCHAIN = "clang"

DEPENDS = "flutter-engine"

LIC_FILES_CHKSUM = "file://LICENSE;md5=3b83ef96387f14655fc854ddc3c6bd57"

S = "${WORKDIR}/git"

inherit pkgconfig cmake

FILES_${PN} = "${bindir}"
