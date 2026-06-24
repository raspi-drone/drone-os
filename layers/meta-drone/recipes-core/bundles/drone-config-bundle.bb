DESCRIPTION = "Drone OS Config Bundle"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://wireguard/wg0.nmconnection \
    file://modem/mm-apn.nmconnection \
    file://ros/mavros_params.yaml \
"

S = "${WORKDIR}"
DEPENDS = "rauc-native e2fsprogs-native"

do_compile() {
    rm -f ${WORKDIR}/drone-config-bundle.raucb
    
    mkdir -p ${WORKDIR}/bundle-input/config-root/wireguard
    mkdir -p ${WORKDIR}/bundle-input/config-root/modem
    mkdir -p ${WORKDIR}/bundle-input/config-root/ros

    install -m 0600 ${WORKDIR}/wireguard/wg0.nmconnection \
        ${WORKDIR}/bundle-input/config-root/wireguard/
    install -m 0600 ${WORKDIR}/modem/mm-apn.nmconnection \
        ${WORKDIR}/bundle-input/config-root/modem/
    install -m 0644 ${WORKDIR}/ros/mavros_params.yaml \
        ${WORKDIR}/bundle-input/config-root/ros/

    cat > ${WORKDIR}/bundle-input/hook.sh << 'HOOK'
#!/bin/sh
case "$1" in
    slot-install)
        umount /config || true
        mkfs.ext4 -L config "$RAUC_SLOT_DEVICE"
        mkdir -p /run/rauc/mnt/config
        mount "$RAUC_SLOT_DEVICE" /run/rauc/mnt/config
        cp -a "$RAUC_BUNDLE_MOUNT_POINT/config-root/." /run/rauc/mnt/config/
        umount /run/rauc/mnt/config
        mount -L config /config
        ;;
    *)
        exit 1
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

[image.config]
hooks=install
MANIFEST

    rauc bundle \
        --cert=${THISDIR}/files/signing.cert.pem \
        --key=${THISDIR}/files/signing.key.pem \
        ${WORKDIR}/bundle-input \
        ${WORKDIR}/drone-config-bundle.raucb
}

do_deploy() {
    install -d ${DEPLOYDIR}
    cp ${WORKDIR}/drone-config-bundle.raucb \
        ${DEPLOYDIR}/drone-config-bundle-${MACHINE}.raucb
}

do_deploy[depends] += "virtual/fakeroot-native:do_populate_sysroot"
addtask deploy after do_compile before do_build
inherit deploy