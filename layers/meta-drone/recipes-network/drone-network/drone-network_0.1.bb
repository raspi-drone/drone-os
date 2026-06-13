SUMMARY = "Drone network configuration"
DESCRIPTION = "Network configuration for drone modem setup"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI += "file://mm-apn.nmconnection"

S = "${WORKDIR}"

do_install() {
    install -d ${D}${sysconfdir}/NetworkManager/system-connections/

    install -m 0600 ${WORKDIR}/mm-apn.nmconnection \
        ${D}${sysconfdir}/NetworkManager/system-connections/mm-apn.nmconnection
}

FILES:${PN} += "${sysconfdir}/NetworkManager/system-connections/mm-apn.nmconnection"