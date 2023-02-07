#
# If not stated otherwise in this file or this component's LICENSE file the
# following copyright and licenses apply:
#
# Copyright 2020 Liberty Global Service B.V.
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

INHIBIT_DEFAULT_DEPS = "1"
DEPENDS += "flutter-cross-${TARGET_ARCH}"
require recipes-devtools/flutter/flutter-common.inc

FLUTTER_PACKAGE      ??= "${PN}/main.dart"

python() {
    import os

    v = d.getVar("FLUTTER_PACKAGE")
    d.setVar("FLUTTER_APP_NAME", v.split("/")[-2])
    d.setVar("FLT_DART_TARGET_NAME_ARG", "--target=" + os.path.join("lib", v.split("/")[-1]))
    d.setVar("FLT_APP_PROJECT_DIR", "/".join(v.split("/")[0:-2]))

    # Helper variables for installing flutter application
    d.setVar("FLT_APP_BASE_DIR",   os.path.join(d.getVar("datadir"),          "flutter/apps", d.getVar("FLUTTER_APP_NAME")))
    d.setVar("FLT_APP_DATA_DIR",   os.path.join(d.getVar("FLT_APP_BASE_DIR"), "data"))
    d.setVar("FLT_APP_ASSETS_DIR", os.path.join(d.getVar("FLT_APP_DATA_DIR"), "flutter_assets"))
    d.setVar("FLT_APP_EXTRAS_DIR", os.path.join(d.getVar("FLT_APP_DATA_DIR"), "flutter_extras"))
    d.setVar("FLT_APP_LIB_DIR",    os.path.join(d.getVar("FLT_APP_BASE_DIR"), "lib"))

    d.setVar("FLUTTER_BIN", os.path.join(d.getVar("STAGING_LIBDIR_NATIVE"), d.getVar("TARGET_SYS"), "flutter/sdk/flutter/bin/flutter"))
}

B = "${S}/${FLT_APP_PROJECT_DIR}/build"
DEBUG_INFO_DIR = "${B}/.debug/symbols/vm_snapshot_symbols"

RUNTIME_MODE = "${@flutter_get_runtime_mode(d)}"

FLUTTER_APP_OBFUSCATE ?= "${@bb.utils.contains('RUNTIME_MODE', 'release', '1', '0', d)}"
OPT_OBFUSCATE = "${FLUTTER_APP_OBFUSCATE}"

FLUTTER_APP_SPLIT_DEBUG_INFO ?= "0"
OPT_SPLIT_DEBUG_INFO = "${FLUTTER_APP_SPLIT_DEBUG_INFO}"

OBFUSCATE_FLAG = "--obfuscate"
SPLIT_DEBUG_INFO_FLAG = "--split-debug-info=${DEBUG_INFO_DIR}"

flutter_do_configure() {
    true
}


flutter_do_compile() {
    if [ "${OPT_OBFUSCATE}" != "0"  ]; then
       OPT="${OBFUSCATE_FLAG} ${SPLIT_DEBUG_INFO_FLAG}"
    elif [ "${OPT_SPLIT_DEBUG_INFO}" != "0" ]; then
       OPT="${SPLIT_DEBUG_INFO_FLAG}"
    fi

    mkdir -p "${DEBUG_INFO_DIR}"

    # Store content of EXTRA_OEFLUTTER variable in a separate file
    echo "${EXTRA_OEFLUTTER}" > ${B}/flutter-extra-options

    (cd ${S}/${FLT_APP_PROJECT_DIR} \
        && ${TARGET_PREFIX}flutter \
                 --${RUNTIME_MODE} \
                 $OPT \
                 ${FLT_DART_TARGET_NAME_ARG} \
                 ${EXTRA_OEFLUTTER} \
                 $@)
}

do_compile[progress] = "outof:^\[(\d+)/(\d+)\]\s+"


flutter_do_install() {
    # Follow the stand-alone flutter directory structure Google is using
    install -p -m 0755 -d ${D}/${FLT_APP_DATA_DIR}/

    # Install application assets
    cp -av --no-preserve=ownership ${B}/flutter_assets \
        ${D}/${FLT_APP_DATA_DIR}/

    # except .last_build_id file
    rm -f ${D}/${FLT_APP_ASSETS_DIR}/.last_build_id

    install -p -m 0755 -D ${B}/lib/libapp.so ${D}/${FLT_APP_LIB_DIR}/libapp.so
    if [ "${OPT_OBFUSCATE}${OPT_SPLIT_DEBUG_INFO}" != "00" ]; then
        # TODO: put the file in debug appmodule package (see Jira: ONEM-21639).
        gzip --to-stdout \
             <"${DEBUG_INFO_DIR}/app.vm_snapshot.symbols" \
             >"${D}/${FLT_APP_LIB_DIR}/app.vm_snapshot.symbols.gz"
    fi

    # Install flutter-extra-options in -extras sub-package
    install -p -m 0644 -D ${B}/flutter-extra-options ${D}/${FLT_APP_EXTRAS_DIR}/flutter-extra-options
}

# dw: Note: There is no runtime dependency on any fluter launcher and that is intentional
# as using virtual provider will limit it to installing only one on the rootfs.

PACKAGES                   += "${PN}-aot ${PN}-jit ${PN}-extras"
PROVIDES                   += "${PN}-aot ${PN}-jit ${PN}-extras"

FILES_${PN}                 = "${FLT_APP_ASSETS_DIR}/asset*"
FILES_${PN}                += "${FLT_APP_ASSETS_DIR}/fonts"
FILES_${PN}                += "${FLT_APP_ASSETS_DIR}/packages"
FILES_${PN}                += "${FLT_APP_ASSETS_DIR}/*.json"
FILES_${PN}                += "${FLT_APP_ASSETS_DIR}/NOTICES*"
RDEPENDS_${PN}              = "${PN}-aot"

FILES_${PN}-aot             = "${FLT_APP_LIB_DIR}/libapp.so"
FILES_${PN}-dbg            += "${FLT_APP_LIB_DIR}/app.vm_snapshot.symbols.gz"
RDEPENDS_${PN}-aot         += "${PN}"
INSANE_SKIP_${PN}-aot      += "ldflags"
INSANE_SKIP_${PN}-aot      += "libdir"

FILES_${PN}-jit             = "${FLT_APP_ASSETS_DIR}/isolate_snapshot_data"
FILES_${PN}-jit            += "${FLT_APP_ASSETS_DIR}/kernel_blob.bin"
FILES_${PN}-jit            += "${FLT_APP_ASSETS_DIR}/vm_snapshot_data"
RDEPENDS_${PN}-jit         += "${PN}"

FILES_${PN}-extras          = "${FLT_APP_EXTRAS_DIR}/"
RDEPENDS_${PN}-extras      += "${PN}"

INSANE_SKIP_${PN}-dbg      += "libdir"

ALLOW_EMPTY_${PN}-dev       = "0"
FILES_${PN}-dev             = ""

EXPORT_FUNCTIONS do_configure do_compile do_install

PRIVATE_LIBS += "libapp.so"
