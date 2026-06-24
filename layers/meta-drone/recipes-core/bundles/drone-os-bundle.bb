DESCRIPTION = "Drone OS Update Bundle"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

S = "${WORKDIR}"

DEPENDS = "rauc-native drone-image"

# Das rootfs.ext4 vom OS-Image als Input
IMAGE_ROOTFS_BUNDLE = "${DEPLOY_DIR_IMAGE}/${IMAGE_BASENAME}-${MACHINE}.rootfs.ext4"
IMAGE_BASENAME = "drone-image"

do_compile() {
    rm -f ${WORKDIR}/drone-os-bundle.raucb
    mkdir -p ${WORKDIR}/bundle-input

    cp ${IMAGE_ROOTFS_BUNDLE} ${WORKDIR}/bundle-input/rootfs.ext4

    cat > ${WORKDIR}/bundle-input/hook.sh << 'HOOK'
#!/bin/sh
case "$1" in
    slot-post-install)
        tune2fs -L rootfs-A "$RAUC_SLOT_DEVICE" 2>/dev/null || true
        ;;
    *)
        exit 0
        ;;
esac
exit 0
HOOK
    chmod +x ${WORKDIR}/bundle-input/hook.sh

    cat > ${WORKDIR}/bundle-input/manifest.raucm << MANIFEST
[update]
compatible=${MACHINE}
version=${DISTRO_VERSION}

[hooks]
filename=hook.sh

[image.rootfs]
filename=rootfs.ext4
hooks=post-install
MANIFEST

    rauc bundle \
        --cert=${THISDIR}/files/signing.cert.pem \
        --key=${THISDIR}/files/signing.key.pem \
        ${WORKDIR}/bundle-input \
        ${WORKDIR}/drone-os-bundle.raucb
}

do_deploy() {
    install -d ${DEPLOYDIR}
    cp ${WORKDIR}/drone-os-bundle.raucb \
        ${DEPLOYDIR}/drone-os-bundle-${MACHINE}.raucb
}

do_deploy[depends] += "virtual/fakeroot-native:do_populate_sysroot"
do_compile[depends] += "drone-image:do_image_complete"
addtask deploy after do_compile before do_build
inherit deploy