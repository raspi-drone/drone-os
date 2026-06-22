SUMMARY = "CycloneDDS unicast config for loopback-only DDS discovery"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"
inherit allarch

SRC_URI = "file://cyclonedds.xml"

do_install() {
    install -d ${D}${sysconfdir}/ros
    install -m 0644 ${WORKDIR}/cyclonedds.xml ${D}${sysconfdir}/ros/cyclonedds.xml
}

FILES:${PN} += "${sysconfdir}/ros/cyclonedds.xml"