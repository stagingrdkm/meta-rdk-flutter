#
# If not stated otherwise in this file or this component's LICENSE file the
# following copyright and licenses apply:
#
# Copyright 2019-2020 Liberty Global Service B.V.
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

SUMMARY     = "Flutter Engine - makes it easy and fast to build beautiful mobile apps"
HOMEPAGE    = "https://github.com/flutter/engine"

LICENSE = "BSD-3-Clause"

inherit python3native

require recipes-devtools/flutter/flutter-common.inc

PV = "${FLUTTER_ENGINE_PV}"

# dw: Note: There is no SRC_URI for flutter engine, only for depot_tools
# We can't use the Google origin as it seems to be very unstable.
# fatal: unable to access 'https://chromium.googlesource.com/chromium/tools/depot_tools.git/': Failed connect to chromium.googlesource.com:443; Operation now in progress
DEPOT_TOOLS_URI ?= "git://github.com/LibertyGlobal/onemw-depot-tools.git"
DEPOT_TOOLS_BRANCH ?= "lgi-2.14.4"
DEPOT_TOOLS_REV ?= "0dc8922acd7f44ac44c129d23a6791836d8091cb"

SRC_URI = "${DEPOT_TOOLS_URI};destsuffix=depot_tools;protocol=https;branch=${DEPOT_TOOLS_BRANCH};rev=${DEPOT_TOOLS_REV}"

# dw: Note: There is no real SRC_URI for flutter as everything
# is synced using gclient and that is how Google designed it.
# To make it easier to rebase to the newer version of engine,
# sources and any other modification on top of it are downloaded from
# https://github.com/LibertyGlobal/flutter-{engine,buildroot} repositories.
# List of patches below (preferably empty) are for code which
# we could not patch using other methods.
# SRC_URI += ""

FLUTTER_ENGINE_SRCREV = "${FLUTTER_ENGINE_REVISION}"

FLUTTER_ENGINE_REPO_URI ?= "https://github.com/LibertyGlobal/flutter-engine.git@lgi-${PV}:${FLUTTER_ENGINE_SRCREV}"

# dw: Note: we need a clang version supporting c++17.
# So far, it's working using version v10.0.0 and v11.0.x.
TOOLCHAIN = "clang"
TOOLCHAIN_class-native = "clang"

FILESEXTRAPATHS_prepend := "${THISDIR}/files:"

inherit pkgconfig

DEPENDS = "rsync-native python3-native ninja-native flutter-gn-native wayland wayland-native freetype ca-certificates"
DEPENDS_append_class-target = " virtual/egl virtual/libgles2 libxkbcommon gtk+3"

# Engine mode: debug, profile, jit_release, release
# release = AOT
PACKAGECONFIG = "${@flutter_get_runtime_mode(d)}"
PACKAGECONFIG[debug] = "--runtime-mode debug --unoptimized"
PACKAGECONFIG[profile] = "--runtime-mode profile --lto"
PACKAGECONFIG[release] = "--runtime-mode release --lto"

FLUTTER_ENGINE_EXTRA_ARGS ?= "--no-build-embedder-examples --no-build-glfw-shell --no-enable-skshaper ${FLUTTER_ENGINE_OPT_ARGS} ${PACKAGECONFIG_CONFARGS}"

GN_EXTRA_ARGS_append_class-native = " ${FLUTTER_ENGINE_EXTRA_ARGS} --full-dart-sdk --disable-desktop-embeddings"
GN_EXTRA_ARGS_append_class-target = " ${FLUTTER_ENGINE_EXTRA_ARGS}"

LIC_FILES_CHKSUM = "file://src/LICENSE;md5=537e0b52077bf0a616d0a0c8a79bc9d5"

S = "${WORKDIR}/engine"

BBCLASSEXTEND = "native"

do_unpack[depends] += "python3-native:do_populate_sysroot"

def generate_custom_toolchain_file(d, in_file_path, out_file_path):
    import string

    with open(in_file_path) as in_file:
        template = string.Template(in_file.read())

        # dw: Note:
        # The -D_FORTIFY_SOURCE option is forcibly disabled to be able to
        # compile debug and unoptimized version of flutter.
        # Later on, it should be safe to re-enable it.
        #
        # -Wno-format-nonliteral is needed for compiling flutter without errors
        # The preference is to not patch soure code as long as possible.
        def options_mangler(s):
            # filter out or modify some options
            rv = s.replace("-D_FORTIFY_SOURCE", "-D_FORTIFY_SOURCE_has_been_disabled")

            # add extra options
            rv = rv + ' -Wno-format-nonliteral'

            return rv


        # __DW_CFLAGS_MARKER - are used only for debugging purposes
        custom_toolchain_vars = {
            'cc':              options_mangler(d.expand('${CC}')
                               + ' -U__DW_CFLAGS_MARKER ' + d.expand('${TARGET_CFLAGS}')),

            'cxx':             options_mangler(d.expand('${CXX}')
                               + ' -U__DW_CXXFLAGS_MARKER ' + d.expand('${TARGET_CXXFLAGS}')
                               + ' -U__DW_CPPFLAGS_MARKER ' + d.expand('${TARGET_CPPFLAGS}')),

          # 'ld':              - dw: Note: there is no need for 'ld' as internally it is identical with 'cxx'.

            'ar':               d.expand('${AR}'),
            'nm':               d.expand('${NM}'),
            'readelf':          d.expand('${TARGET_PREFIX}readelf'),
            'strip':            d.expand('${TARGET_PREFIX}strip'),
            'custom_lib_flags': d.expand('${TARGET_LDFLAGS}'),
        }

        with open(out_file_path, 'w') as out_file:
            out_file.write('# This file has been generated automatically from {}.\n\n'.format(in_file_path))
            out_file.write(template.substitute(custom_toolchain_vars))


# Used by flutter-cache bbclass implementation {
# no-op in vanilla yocto environment
ONEMW_CACHE_NAME = "flutter-engine-git-cache"
ONEMW_CACHE_VERSION = "${PV}-${FLUTTER_ENGINE_SRCREV}"
ONEMW_CACHE_FETCH_BEFORE = "do_unpack"
ONEMW_CACHE_PUSH_AFTER = "do_unpack"
inherit flutter-cache
# }

do_unpack_flutter() {
  DEPOT_TOOLS=${S}/../depot_tools
  FLUTTER_REPO="${FLUTTER_ENGINE_REPO_URI}"
  ENGINE_PATH=${S}

  # Setup git cache
  export GIT_CACHE_PATH=${ONEMW_CACHE_LOCAL_PATH}
  mkdir -p "${GIT_CACHE_PATH}"

  # Following steps are based on: https://github.com/flutter/flutter/wiki/Setting-up-the-Engine-development-environment

  #Prerequisites (Chromium's depot_tools).
  #Note: The repo will be cloned only when using do_unpack outside of the yocto build system
  #      as the flutter-engine.bb recipe uses SRC_URI for cloning depot_tools.
  [ -d ${DEPOT_TOOLS} ] || git clone ${DEPOT_TOOLS_URI} --branch ${DEPOT_TOOLS_BRANCH} --single-branch ${DEPOT_TOOLS}
  export PATH=$PATH:${DEPOT_TOOLS}

  #3
  # Removing any existing directory prevents the following error
  # | Your .gclient file seems to be broken. The requested URL is different from what
  # | is actually checked out in engine/src/flutter.
  rm -rf ${ENGINE_PATH}
  mkdir -p ${ENGINE_PATH}

  #4
  pushd ${ENGINE_PATH}
  echo -n "solutions =" > .gclient
  echo "[{\"managed\": \"False\",\"name\": \"src/flutter\",\"url\": \"${FLUTTER_REPO}\",\"deps_file\": \"DEPS\",\
          \"custom_vars\": {\"download_android_deps\" : \"False\", \"download_windows_deps\" : \"False\"}, \
          \"custom_deps\": {\"src\": \"${FLUTTER_BUILDROOT_URI}\"}}]" \
    | python3 -m json.tool >> .gclient

  #5
  # zk fix this - temporary disable google vpython since it has problem with dunfell python2
  #
  # | ERROR: The executable /home/user/.vpython-root/cc68d5/bin/python2 is not functioning
  # | ERROR: It thinks sys.prefix is u'/usr' (should be u'/home/user/.vpython-root/cc68d5')
  # | ERROR: virtualenv is not compatible with this system or executable
  export VPYTHON_BYPASS="manually managed python not supported by chrome operations"

  # zk fix this - after disabling vpython, need to disable python ssl cert validation
  export PYTHONHTTPSVERIFY=0

  ln -sf /usr/bin/curl
  export PATH=$PWD:$PATH
  GCLIENT_PY3=1 GCLIENT_TEST=1 python3 $(which gclient.py) sync --no-history ${PARALLEL_MAKE} -v
}
do_unpack_flutter[lockfiles] = "${ONEMW_CACHE_LOCAL_PATH_LOCK}"


# prepend do_configure() task by overwriting custom toolchain file {

python do_overwrite_toolchain_file () {
    s_dir = d.expand("${S}")
    in_file_path  = os.path.join(s_dir, "src/build/toolchain/custom/BUILD.gn.template")
    out_file_path = os.path.join(s_dir, "src/build/toolchain/custom/BUILD.gn")
    generate_custom_toolchain_file(d, in_file_path, out_file_path)
}

addtask overwrite_toolchain_file after do_patch before do_configure

addtask unpack_flutter after do_unpack before do_overwrite_toolchain_file do_populate_lic
# }


do_configure() {
  # Ensures that previous output directory gets removed
  rm -rf ${S}/src/out

  # Note: We just need to pass non-empty --target-toolchain=
  # however, the value itself is meaningless as we are
  # generating the complete custom toolchain file
  # see generate_custom_toolchain_file() for more details

  target_cpu="${@flutter_get_target_arch_name(d)}"

  # The ./src/flutter/third_party/gn/gn is a binary blob which requires newer glibc
  # than we have. Instead use flutter-gn which we compiled from sources.
  rm -f ./src/flutter/third_party/gn/gn
  ln -sf $(which flutter-gn) ./src/flutter/third_party/gn/gn

  export PATH=$PWD:$PATH

  ./src/flutter/tools/gn \
    ${GN_EXTRA_ARGS} \
    --target-os=linux \
    --linux-cpu="${target_cpu}" \
    --target-triple="${HOST_SYS}" \
    --enable-fontconfig \
    --embedder-for-target \
    --target-toolchain=target-toolchain-meaningless-value
}


do_compile() {
  target_cpu="${@flutter_get_target_arch_name(d)}"

  cd ${S}/src/out
  ln -sf $(find . -maxdepth 1 -type d  | grep ${target_cpu}) output
  cd output

  export PATH=$PWD:$PATH
  ninja -v ${PARALLEL_MAKE}
}

do_compile_append_class-target () {
  ninja -v ${PARALLEL_MAKE} dart_precompiled_runtime_product
}

do_compile[progress] = "outof:^\[(\d+)/(\d+)\]\s+"


do_install() {
  # dw Note: Due to differences in ${GN_EXTRA_ARGS} between -target and -native
  # some of the artifacts might or might not be available, thus we use || true.

# flutter-engine {
  install -p -D -m 0644 ${S}/src/out/output/icudtl.dat \
      ${D}${datadir}/flutter/icudtl.dat # TODO switch to use system icu library
# }

# flutter-engine-engine {
  install -p -D -m 0755 ${S}/src/out/output/so.unstripped/libflutter_engine.so \
      ${D}${libdir}/libflutter_engine.so
# }

# flutter-engine-dev {
  install -p -d -m 0755 ${D}${includedir}/flutter/
  install -p -m 0644 \
      ${S}/src/out/output/flutter_embedder.h \
      ${D}${includedir}/flutter/

  cp -av --no-preserve=ownership ${S}/src/out/output/flutter_linux \
      ${D}${includedir}/flutter/ || true

  install -d -m 0755 ${D}${libdir}/pkgconfig/
  cat > ${D}${libdir}/pkgconfig/flutter-engine.pc << EOD
# flutter-engine pkg-config file

_prefix=/usr
_exec_prefix=\${_prefix}
_libdir=\${_prefix}/lib
_includedir=\${_prefix}/include

Name: ${PN}
Description: Flutter Engine - makes it easy and fast to build beautiful mobile apps
Version: ${PV}
URL: https://github.com/flutter/engine
Conflicts:
Libs: -L\${_libdir} -lflutter_engine
Cflags: -I\${_includedir}/flutter
EOD

  cat > ${D}${libdir}/pkgconfig/flutter-linux-gtk.pc << EOD
# flutter-linux-gtk pkg-config file

_prefix=/usr
_exec_prefix=\${_prefix}
_libdir=\${_prefix}/lib
_includedir=\${_prefix}/include

Name: ${PN}
Description: Flutter Linux GTK backend
Version: ${PV}
URL: https://github.com/flutter/engine
Requires: gtk+-3.0
Requires.private: flutter-engine
Conflicts:
Libs: -L\${_libdir} -lflutter_linux_gtk
Cflags: -I\${_includedir}/flutter
EOD
# }


# flutter-engine-sdk {
  install -p -d -m 0755 ${D}${libdir}/flutter/engine/sdk/
  cp -av --no-preserve=ownership \
      ${S}/src/out/output/dart-sdk \
          ${D}${libdir}/flutter/engine/sdk/ || true

  # Fixes: dartaotruntime from flutter-engine-native was already stripped, this will prevent future debugging!
  rm -f ${D}${libdir}/flutter/engine/sdk/dart-sdk/bin/dartaotruntime

  # Fixes: libtensorflowlite_c-linux64.so from flutter-engine-native was already stripped, this will prevent future debugging!
  rm -f ${D}${libdir}/flutter/engine/sdk/dart-sdk/bin/snapshots/libtensorflowlite_c-linux64.so

  # Fixes: dart-sdk/bin/dart' from flutter-engine-native was already stripped, this will prevent future debugging!
  install -p -D -m 0755 ${S}/src/out/output/exe.unstripped/dart \
      ${D}${libdir}/flutter/engine/sdk/dart-sdk/bin/dart || true

  # Fixes: dart-sdk/bin/utils/gen_snapshot' from flutter-engine-native was already stripped, this will prevent future debugging!
  install -p -D -m 0755 ${S}/src/out/output/exe.unstripped/gen_snapshot_product \
      ${D}${libdir}/flutter/engine/sdk/dart-sdk/bin/utils/gen_snapshot || true

  # Install the cross version of gen_snapshot (note it is host-native executable generated for building -target package)
  # We cannot install properly the executable version which contains debug symbols:
  #
  #   install -p -D -m 0755 ${S}/src/out/output/clang_x64/exe.unstripped/gen_snapshot
  #
  # as Yocto will try to strip it using -target toolchain and will complain with:
  #
  #   gen_snapshot: File format not recognized
  #   ERROR: flutter-engine-2.10.0-132.0.dev-r0 do_package: Function failed: split_and_strip_files
  #
  # so cheat the Yocto by installing already stripped version. However, this generates
  # another type of warnings which we will mute by using INSANE_SKIP for [arch], [file-rdeps]
  # and [already-stripped].
  #
  # Happily those INSANE_SKIP warnings we be muted only for the -sdk sub-package which is not supposed
  # to be installed on the -target platform. Thanks to this, we are not introducing the risk of
  # creating a package with broken list of dependencies which will be installed on the -target platform.
  #
  install -p -D -m 0755 ${S}/src/out/output/clang_x64/gen_snapshot \
      ${D}${libdir}/flutter/engine/sdk/dart-sdk/bin/utils/gen_snapshot || true

  # Install dart_precompiled_runtime_product for launching dart snapshots
  install -p -D -m 0755 ${S}/src/out/output/dart_precompiled_runtime_product \
      ${D}${libdir}/flutter/engine/sdk/dart-sdk/bin/dart_precompiled_runtime_product || true

  cp -av --no-preserve=ownership \
      ${S}/src/out/output/flutter_patched_sdk \
          ${D}${libdir}/flutter/engine/sdk/

  install -p -d -m 0755 ${D}${libdir}/flutter/engine/sdk/gen/
  cp -av --no-preserve=ownership \
      ${S}/src/out/output/gen/flutter \
          ${D}${libdir}/flutter/engine/sdk/gen/

  install -p -D -m 0755 ${S}/src/out/output/gen/frontend_server.dart.snapshot \
      ${D}${libdir}/flutter/engine/sdk/gen/ || true
# }

# flutter-engine-gtk {
  install -p -D -m 0755 ${S}/src/out/output/so.unstripped/libflutter_linux_gtk.so \
      ${D}${libdir}/libflutter_linux_gtk.so || true
# }
}

PACKAGES                            += "${PN}-engine ${PN}-gtk ${PN}-sdk"

PROVIDES                            += "${PN}-engine ${PN}-gtk ${PN}-sdk"

FILES_${PN}                          = "${datadir}/flutter/icudtl.dat"

FILES_${PN}-engine                   = "${libdir}/libflutter_engine.so"
RDEPENDS_${PN}-engine               += "${PN}"

FILES_${PN}-gtk                      = "${libdir}/libflutter_linux_gtk.so"
FILES_${PN}-gtk-dev                  = ""
RDEPENDS_${PN}-gtk                  += "${PN}"

FILES_${PN}-dev                      = "${includedir}/flutter"
FILES_${PN}-dev                     += "${libdir}/pkgconfig/"
RDEPENDS_${PN}-dev                  += "${PN}-engine"
RDEPENDS_${PN}-dev                  += "${PN}-gtk"

FILES_${PN}-sdk                     += "${libdir}/flutter/"
INSANE_SKIP_${PN}-sdk               += "arch file-rdeps"
INSANE_SKIP_${PN}                   += "already-stripped"
