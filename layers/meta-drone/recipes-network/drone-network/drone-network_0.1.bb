SUMMARY = "Drone network configuration"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI += " \
    file://mm-apn.nmconnection \
    file://wg0.nmconnection \
    file://modem.conf \
    file://90-wireguard \
"

S = "${WORKDIR}"

do_install() {
    # NetworkManager connection
    install -d ${D}${sysconfdir}/NetworkManager/system-connections

    install -m 0600 ${WORKDIR}/mm-apn.nmconnection \
        ${D}${sysconfdir}/NetworkManager/system-connections/mm-apn.nmconnection

    # WireGuard connection
    install -m 0600 ${WORKDIR}/wg0.nmconnection \
        ${D}${sysconfdir}/NetworkManager/system-connections/wg0.nmconnection


    # NetworkManager dispatcher
    install -d ${D}${sysconfdir}/NetworkManager/dispatcher.d

    install -m 0755 ${WORKDIR}/90-wireguard \
        ${D}${sysconfdir}/NetworkManager/dispatcher.d/90-wireguard


    # NetworkManager systemd drop-in
    install -d ${D}${systemd_system_unitdir}/NetworkManager.service.d

    install -m 0644 ${WORKDIR}/modem.conf \
        ${D}${systemd_system_unitdir}/NetworkManager.service.d/modem.conf
}

FILES:${PN} += " \
    ${sysconfdir}/NetworkManager/system-connections/mm-apn.nmconnection \
    ${sysconfdir}/NetworkManager/system-connections/wg0.nmconnection \
    ${sysconfdir}/NetworkManager/dispatcher.d/90-wireguard \
    ${systemd_system_unitdir}/NetworkManager.service.d/modem.conf \
"