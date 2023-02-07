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

SUMMARY          = "A slide (15) puzzle implemented in Dart and Flutter"
HOMEPAGE         = "https://github.com/kevmoo/slide_puzzle"
LICENSE          = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=9ed7fa223aea2ee9fbb92006a4621a59"
SRC_URI          = "git://github.com/kevmoo/slide_puzzle.git;destdir=flutter-examples--slide_puzzle;protocol=https;branch=master;rev=e4d0f8a0b15f5a6d8228a852906d002e0844b166"
S                = "${WORKDIR}/git"

inherit flutter

FLUTTER_PACKAGE  = "slide_puzzle/main.dart"
