SUMMARY = "Drone cellular/modem configuration"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI += " \
    file://mm-apn.nmconnection \
    file://modem.conf \
"

S = "${WORKDIR}"

do_install() {
    # NetworkManager connection for the cellular modem (GSM/QMI)
    install -d ${D}${sysconfdir}/NetworkManager/system-connections
    install -m 0600 ${WORKDIR}/mm-apn.nmconnection \
        ${D}${sysconfdir}/NetworkManager/system-connections/mm-apn.nmconnection

    # NetworkManager systemd drop-in: wait for ModemManager
    install -d ${D}${systemd_system_unitdir}/NetworkManager.service.d
    install -m 0644 ${WORKDIR}/modem.conf \
        ${D}${systemd_system_unitdir}/NetworkManager.service.d/modem.conf
}

FILES:${PN} += " \
    ${sysconfdir}/NetworkManager/system-connections/mm-apn.nmconnection \
    ${systemd_system_unitdir}/NetworkManager.service.d/modem.conf \
"