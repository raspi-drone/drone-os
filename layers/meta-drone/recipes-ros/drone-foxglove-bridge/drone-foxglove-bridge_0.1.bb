SUMMARY = "Foxglove Bridge service (managed by meta-drone)"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI += " \
    file://foxglove-bridge.service \
"

S = "${WORKDIR}"

inherit systemd

SYSTEMD_SERVICE:${PN} = "foxglove-bridge.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

RDEPENDS:${PN} += "foxglove-bridge"

do_install() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/foxglove-bridge.service \
        ${D}${systemd_system_unitdir}/foxglove-bridge.service
}

FILES:${PN} += " \
    ${systemd_system_unitdir}/foxglove-bridge.service \
"