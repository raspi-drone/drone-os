SUMMARY = "Drone WireGuard configuration"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI += " \
    file://wg0.nmconnection \
    file://90-wireguard \
"

S = "${WORKDIR}"

# Soft dependency: the dispatcher reacts to the modem interface (wwan0)
# coming up, but doesn't hard-fail if drone-modem isn't installed.
RRECOMMENDS:${PN} += "drone-modem"

do_install() {
    # WireGuard connection
    install -d ${D}${sysconfdir}/NetworkManager/system-connections
    install -m 0600 ${WORKDIR}/wg0.nmconnection \
        ${D}${sysconfdir}/NetworkManager/system-connections/wg0.nmconnection

    # NetworkManager dispatcher: bring up wg0 once the modem (wwan0) is up
    install -d ${D}${sysconfdir}/NetworkManager/dispatcher.d
    install -m 0755 ${WORKDIR}/90-wireguard \
        ${D}${sysconfdir}/NetworkManager/dispatcher.d/90-wireguard
}

FILES:${PN} += " \
    ${sysconfdir}/NetworkManager/system-connections/wg0.nmconnection \
    ${sysconfdir}/NetworkManager/dispatcher.d/90-wireguard \
"