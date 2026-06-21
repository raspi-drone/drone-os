FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += " \
    file://nftables.conf \
    file://nftables.service \
"

inherit systemd

SYSTEMD_SERVICE:${PN} = "nftables.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install:append() {
    install -m 0644 ${WORKDIR}/nftables.conf ${D}${sysconfdir}/nftables.conf

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/nftables.service \
        ${D}${systemd_system_unitdir}/nftables.service
}

FILES:${PN} += " \
    ${sysconfdir}/nftables.conf \
    ${systemd_system_unitdir}/nftables.service \
"

CONFFILES:${PN} += "${sysconfdir}/nftables.conf"