SUMMARY = "GeographicLib geoid datasets required by MAVROS"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit allarch

SRC_URI = "https://downloads.sourceforge.net/project/geographiclib/geoids-distrib/egm96-5.tar.bz2"
SRC_URI[sha256sum] = "c46224f8f723dc915d97179f4e1580a98d6c742fe2b82cd8fef0ecaaad13e614"

# Prevent bitbake from cd-ing into a nonexistent S directory
S = "${WORKDIR}"

do_install() {
    install -d ${D}/usr/share/GeographicLib/geoids
    pgm=$(find ${WORKDIR} -name "egm96-5.pgm" | head -1)
    if [ -z "$pgm" ]; then
        bbfatal "egm96-5.pgm not found after unpack — check tarball structure"
    fi
    install -m 0644 "$pgm" ${D}/usr/share/GeographicLib/geoids/egm96-5.pgm
}

FILES:${PN} = "/usr/share/GeographicLib/geoids"