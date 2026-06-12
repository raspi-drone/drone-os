DESCRIPTION = "Drone OS Image"
LICENSE = "MIT"

inherit core-image

IMAGE_INSTALL += " \
    packagegroup-core-boot \
    kernel-modules \
    networkmanager \
    modemmanager \
    wireguard-tools \
"