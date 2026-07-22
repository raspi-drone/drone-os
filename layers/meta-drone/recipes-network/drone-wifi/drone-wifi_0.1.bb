SUMMARY = "Drone WiFi configuration"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI += " \
    file://wifi.nmconnection \
"

S = "${WORKDIR}"

# NetworkManager needs wpa_supplicant to manage WiFi connections.
RDEPENDS:${PN} += "networkmanager-wifi"

do_install() {
    # NetworkManager connection for WiFi. Used as a low-priority fallback
    # data path: it only carries traffic while the cellular modem (wwan0)
    # is unavailable, see route-metric in wifi.nmconnection.
    install -d ${D}${sysconfdir}/NetworkManager/system-connections
    install -m 0600 ${WORKDIR}/wifi.nmconnection \
        ${D}${sysconfdir}/NetworkManager/system-connections/wifi.nmconnection
}

FILES:${PN} += " \
    ${sysconfdir}/NetworkManager/system-connections/wifi.nmconnection \
"
