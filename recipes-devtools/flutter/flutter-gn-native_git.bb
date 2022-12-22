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
# Author: Michal Kucharczyk <mkucharczyk.contractor@libertyglobal.com>
#

SUMMARY = "GN is a meta-build system that generates build files for Ninja."
HOMEPAGE = "https://gn.googlesource.com/gn/"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${WORKDIR}/git/LICENSE;md5=0fca02217a5d49a14dfe2d11837bb34d"

DEPENDS = "ninja-native clang-native"

TOOLCHAIN_class-native = "clang"

inherit native

SRCREV="0153d369bbccc908f4da4993b1ba82728055926a"

SRC_URI = "git://gn.googlesource.com/gn;protocol=https"

do_configure[noexec] = "1"

do_compile() {
    python3 ${WORKDIR}/git/build/gen.py
    ninja -C ${WORKDIR}/git/out
}

do_install() {
    install -p -m 0755 -D ${WORKDIR}/git/out/gn ${D}${bindir}/flutter-gn
}

INSANE_SKIP_${PN} += "already-stripped"
