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

SUMMARY     = "Flutter tools (packager frontend)"
HOMEPAGE    = "https://github.com/flutter/flutter"

LICENSE = "BSD-3-Clause"

require recipes-devtools/flutter/flutter-common.inc

inherit cross staging

PV       = "${FLUTTER_PV}"
PN       = "flutter-cross-${TARGET_ARCH}"
BPN      = "flutter"

FLUTTER_CROSS_URI ?= "git://github.com/LibertyGlobal/flutter"
FLUTTER_CROSS_BRANCH ?= "lgi-${PV}"
SRCREV = "${FLUTTER_SRCREV}"

SRC_URI = "${FLUTTER_CROSS_URI};destsuffix=flutter.git;protocol=https;branch=${FLUTTER_CROSS_BRANCH};bareclone=1"
SRC_URI += "file://flutter-cross"
SRC_URI += "file://flutter.template"

# Ignore how TARGET_ARCH is computed.
TARGET_ARCH[vardepvalue] = "${TARGET_ARCH}"

INHIBIT_DEFAULT_DEPS = "1"
INHIBIT_SYSROOT_STRIP = "1"
DEPENDS = "curl-native rsync-native flutter-engine flutter-engine-native"

LIC_FILES_CHKSUM = "file://LICENSE;md5=1d84cf16c48e571923f837136633a265"

S = "${WORKDIR}/sdk/flutter"

# dw: The "engine/src/out" is in pair with the flutter expectations
FLUTTER_SDK_DIR="${D}${libdir}/flutter/sdk"
OUT_DIR="${FLUTTER_SDK_DIR}/engine/src/out"
FLUTTER_RUNTIME_MODE="${@flutter_get_runtime_mode(d)}"

python() {
    import os

    dir_prefix = flutter_get_artifact_dir_prefix(d)

    sdk_host_dir = dir_prefix + flutter_get_host_arch_name(d)
    d.setVar("SDK_HOST_DIR", sdk_host_dir)

    sdk_target_dir = dir_prefix + flutter_get_target_arch_name(d)
    d.setVar("SDK_TARGET_DIR", sdk_target_dir)
}

do_unpack_from_git[cleandirs] += "${WORKDIR}/sdk/flutter"

do_unpack_from_git() {
    # Repository is checked out with bare option to not use objects from reference repository (in downloads folder).
    # This is to avoid below git errors when building flutter application using flutter-sdk docker.
    # error:
    #   Failed to find the latest git commit date: VersionCheckError:
    #   Command exited with code 128: git -c log.showSignature=false log -n 1 --pretty=format:%ad --date=iso
    git clone -b ${FLUTTER_CROSS_BRANCH} --dissociate ${WORKDIR}/flutter.git ${WORKDIR}/sdk/flutter
    cd ${WORKDIR}/sdk/flutter
    git reset --hard ${SRCREV}
}


addtask unpack_from_git after do_unpack before do_patch

do_configure() {
}


do_compile() {
    # Fetch tags to avoid the following error:
    #     The current Flutter SDK version is 0.0.0-unknown.
    git fetch --tags
}


do_install_flutter() {
    echo "[1/11] installing flutter"
    install -p -m 0755 -d "${FLUTTER_SDK_DIR}/"
    rsync -az "${S}" \
        "${FLUTTER_SDK_DIR}/"

    echo "[2/11] installing cross script"
    FLUTTER_CROSS="${FLUTTER_SDK_DIR}/flutter/bin/${TARGET_PREFIX}flutter"
    install -p -m 0755 -D "${S}/../../flutter-cross" "${FLUTTER_CROSS}"

    # install symlink
    install -p -m 0755 -d "${D}${bindir}"
    rm -f ${D}${bindir}/${TARGET_PREFIX}flutter
    lnr "${FLUTTER_CROSS}" "${D}${bindir}/${TARGET_PREFIX}flutter"

    echo "[3/11] flutter installation finished"
}


do_install_sdk() {
    echo "[4/11] creating sdk directory for host: ${SDK_HOST_DIR}, target: ${SDK_TARGET_DIR}"

    install -p -m 0755 -d "${OUT_DIR}/${SDK_HOST_DIR}/"

    echo "[5/11] installing host sdk"
    rsync -a "${STAGING_LIBDIR_NATIVE}/flutter/engine/sdk/" \
        "${OUT_DIR}/${SDK_HOST_DIR}/"

    echo "[6/11] installing the cross version of gen_snapshot"
    rsync -a "${STAGING_DIR_TARGET}/usr/lib/flutter/engine/sdk/dart-sdk/bin/utils/gen_snapshot" \
        "${OUT_DIR}/${SDK_HOST_DIR}/"

    echo "[7/11] installing target sdk"
    install -p -m 0755 -d "${OUT_DIR}/${SDK_TARGET_DIR}/"

    rsync -a "${STAGING_DIR_TARGET}/usr/lib/flutter/engine/sdk/" \
        "${OUT_DIR}/${SDK_TARGET_DIR}/"

    # dw: TODO: workaround for flutter --local-engine issue
    (cd ${OUT_DIR}; [ -L host_${FLUTTER_RUNTIME_MODE} ] || ln -sf ${SDK_HOST_DIR} host_${FLUTTER_RUNTIME_MODE})

    echo "[8/11] sdk installation finished"
}


do_install_sdk_sanity_test() {
    APP_DIR=${WORKDIR}
    APP_NAME=flutter_test_app
    APP_MAIN_DART="lib/main_alt.dart"
    FLUTTER_BIN=${FLUTTER_SDK_DIR}/flutter/bin/flutter

    # dw: curl-native on dunfell seems to be entirely broken
    # so it's seems to be safer to use host native version instead.
    #
    # curl: (77) error setting certificate verify locations:
    #|   CAfile: /data/dwrobel1/projects/onemw/7218c-dunfell-h/onemw/build-brcm972180hbc-refboard/tmp/work/x86_64-linux/curl-native/7.69.1-r0/recipe-sysroot-native/etc/ssl/certs/ca-certificates.crt
    #|   CApath: none
    #|
    #| Failed to retrieve the Dart SDK from: https://storage.googleapis.com/flutter_infra_release/flutter/f0826da7ef2d301eb8f4ead91aaf026aa2b52881/dart-sdk-linux-x64.zip
    (cd ${WORKDIR} && ln -sf /usr/bin/curl)
    export PATH=${WORKDIR}:$PATH

    echo "[9/11] create sample application using: ${FLUTTER_BIN}"
    (cd ${APP_DIR}; ${FLUTTER_BIN} --local-engine=${SDK_HOST_DIR} create -v ${APP_NAME})
    # dw: Rename the source file - so we will verify if we could compile projects using --target option
    (cd ${APP_DIR}; mv ${APP_NAME}/lib/main.dart ${APP_NAME}/${APP_MAIN_DART})
    echo "[10/11] cross compile application"
    (cd ${APP_DIR}/${APP_NAME}; "${D}${bindir}/${TARGET_PREFIX}flutter" --${FLUTTER_RUNTIME_MODE} --target=${APP_MAIN_DART} --dart-define=__TEST_VARIABLE_1=__test_value_1)
    echo "[11/11] sanity test installation finished"
}


do_install() {
    do_install_flutter
    do_install_sdk
    do_install_sdk_sanity_test
}

do_install[progress]   = "outof:^\[(\d+)/(\d+)\]\s+"
do_install[cleandirs] += "${WORKDIR}/flutter_test_app"
do_install[cleandirs] += "${FLUTTER_SDK_DIR}"

FILES_${PN}            = "${bindir}/"
FILES_${PN}           += "${libdir}/"
