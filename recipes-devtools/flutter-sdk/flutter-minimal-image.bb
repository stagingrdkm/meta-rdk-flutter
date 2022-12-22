IMAGE_FEATURES = ""
EXTRA_IMAGE_FEATURES = ""
KERNELDEPMODDEPEND = ""
ONEMW_INITRAMFS_IMAGE = ""
export ONEMW_INITRAMFS_IMAGE = ""

DISTRO_FEATURES_remove = "systemd"
IMAGE_INSTALL = "flutter-engine-engine flutter-launcher-wayland"

DEPENDS = "flutter-cross-${TARGET_ARCH}"

inherit image
IMAGE_FSTYPES = "tar.gz"

do_build[depends] = ""

PACKAGE_INSTALL_remove = "busybox-devel"
