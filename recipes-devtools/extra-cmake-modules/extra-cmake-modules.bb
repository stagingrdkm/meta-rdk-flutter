SUMMARY = "Extra modules and scripts for CMake"
LICENSE = "BSD"
LIC_FILES_CHKSUM = "file://COPYING-CMAKE-SCRIPTS;md5=54c7042be62e169199200bc6477f04d1"

PV = "5.76.0+gitr${SRCPV}"
SRCREV = "2135cbdfa6da743f32f3d03b0661313caecc7b16"

SRC_URI = " \
    git://github.com/KDE/extra-cmake-modules;protocol=https;branch=master \
"

S = "${WORKDIR}/git"

EXTRA_OECMAKE += "-DBUILD_TESTING=off"

inherit cmake

FILES_${PN}-dev += "${datadir}/ECM"
