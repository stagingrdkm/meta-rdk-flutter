SHELL = /bin/bash
TOP_DIR = ../../../..
# Preferably keep it in sync with FLUTTER_PR
SDK_RELEASE ?= r59

SDK_PATH = $(shell [ -d $(TOP_DIR)/build*/tmp/sysroots/x86_64-linux/usr/lib/arm-*-linux-gnueabi/flutter/sdk ] \
         && readlink -f $(TOP_DIR)/build*/tmp/sysroots/x86_64-linux/usr/lib/arm-*-linux-gnueabi/flutter/sdk \
         || readlink -f $(TOP_DIR)/build*/tmp/sysroots-components/x86_64/flutter-cross-arm/usr/lib/arm-*-linux-gnueabi/flutter/sdk)
CONF_PATH = $(shell readlink -f $(TOP_DIR)/build*/conf/local.conf)

check_defined = \
	$(strip $(foreach 1,$1, \
	$(call __check_defined,$1,$(strip $(value 2)))))
__check_defined = \
	$(if $(value $1),, \
	$(error Undefined $1$(if $2, ($2))))
$(call check_defined, SDK_PATH CONF_PATH, path to build artifacts)

MACHINE = $(shell sed -nr 's/^MACHINE\s*=\s*"(.+)"/\1/p' $(CONF_PATH) | sed '$$!d')
SDK_VERSION = $(shell cat $(TOP_DIR)/build*/tmp/{sysroots/x86_64-linux,sysroots-components/x86_64/flutter-cross-arm}/usr/lib/arm-*-linux-gnueabi/flutter/sdk/flutter/version 2>/dev/null || true)

SDK_TYPE = $(shell sed -nr 's/^FLUTTER_ENGINE_RUNTIME_MODE\s*=\s*"(.+)"/\1/p' $(CONF_PATH) | sed '$$!d')
IMAGE_NAME ?= flutter-wrapper
IMAGE_REPO ?= quay.io/d_wrobel
DOCKER ?= $(shell which podman || which docker)
$(call check_defined, MACHINE SDK_VERSION SDK_TYPE, image variables)

ROOTFS_PATH=$(shell readlink -f $(TOP_DIR)/build*/tmp/deploy/images/*/flutter-minimal-image-$(MACHINE).tar.gz)
$(call check_defined, ROOTFS_PATH, path to build artifacts)

ifeq ($(BASE_IMAGE),)
	IMAGE_TAG_LATEST = $(SDK_TYPE)-latest
	IMAGE_TAG = $(SDK_VERSION)-$(SDK_TYPE)-$(SDK_RELEASE)
	BASE_IMAGE = fedora:32
else
	IMAGE_TAG_LATEST = latest
	IMAGE_TAG = $(SDK_VERSION)-$(SDK_RELEASE)
endif

.PHONY: image
image:
	ln -sf $(SDK_PATH) -T sdk
	mkdir -p files/$(SDK_TYPE)/
	tar zxf $(ROOTFS_PATH) -C files/$(SDK_TYPE) ./usr/bin/flutter-launcher-wayland \
		./usr/lib/libflutter_engine.so ./usr/share/flutter/icudtl.dat
	tar -czh . | $(DOCKER) build --build-arg SDK_RELEASE=$(SDK_RELEASE) --build-arg BASE_IMAGE=$(BASE_IMAGE) --tag $(IMAGE_NAME):$(IMAGE_TAG) -
	$(DOCKER) tag $(IMAGE_NAME):$(IMAGE_TAG) $(IMAGE_NAME):$(IMAGE_TAG_LATEST)
	$(DOCKER) tag $(IMAGE_NAME):$(IMAGE_TAG) $(IMAGE_REPO)/$(IMAGE_NAME):$(IMAGE_TAG)
	$(DOCKER) tag $(IMAGE_NAME):$(IMAGE_TAG) $(IMAGE_REPO)/$(IMAGE_NAME):$(IMAGE_TAG_LATEST)
	rm -rf sdk files
