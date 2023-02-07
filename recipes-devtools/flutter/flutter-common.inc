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

require recipes-devtools/flutter/flutter-version.inc

PR = "${FLUTTER_PR}"

# dw: A stopgap for: https://github.com/dart-lang/pub/issues/1178
FLUTTER_PUB_CACHE ??= "${WORKDIR}/temp/.pub-cache"
export PUB_CACHE="${FLUTTER_PUB_CACHE}"


# dw: Note: I have not seen mips support in the Dart engine, so far.
FLUTTER_ENGINE_TARGET_ARCH_NAME_aarch64 = "arm64"
FLUTTER_ENGINE_TARGET_ARCH_NAME_arm     = "arm"
FLUTTER_ENGINE_TARGET_ARCH_NAME_x86     = "x86"
FLUTTER_ENGINE_TARGET_ARCH_NAME_x86-64  = "x64"
FLUTTER_ENGINE_RUNTIME_MODE ?= "${@bb.utils.contains("MACHINE_FEATURES", "debug", "profile", "release", d)}"

def clang_install_path(d):
    """Return clang compiler install path."""
    return d.getVar("STAGING_BINDIR_NATIVE")


def flutter_get_target_arch_name(d):
    name = d.getVar("FLUTTER_ENGINE_TARGET_ARCH_NAME")

    if name is None:
        bb.fatal('Unsupported FLUTTER_ENGINE_TARGET_ARCH_NAME target CPU architecture.')
    return name


def flutter_get_host_arch_name(d):
    import re

    arch_translations = {
        r'aarch64.*': 'arm64',
        r'arm.*': 'arm',
        r'i[3456]86$': 'x86',
        r'x86_64$': 'x64',
    }

    build_arch = d.getVar("BUILD_ARCH")

    for arch_regexp, gn_arch_name in arch_translations.items():
        if re.match(arch_regexp, build_arch):
            return gn_arch_name

    bb.fatal('Unsuported BUILD_ARCH value: "%s"' % build_arch)

def flutter_get_runtime_mode(d):
    mode = d.getVar("FLUTTER_ENGINE_RUNTIME_MODE")
    if mode not in 'profile release':
      bb.fatal('Unsupported FLUTTER_ENGINE_RUNTIME_MODE.')
    return mode

def flutter_get_artifact_dir_prefix(d):
    mode = flutter_get_runtime_mode(d)

    if 'debug' in mode:
        artifact_dir = 'linux_debug_unopt_'
    elif 'profile' in mode:
        artifact_dir = 'linux_profile_'
    else:
        artifact_dir = 'linux_release_'

    return artifact_dir

