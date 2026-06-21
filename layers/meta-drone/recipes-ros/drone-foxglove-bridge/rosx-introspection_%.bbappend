# rosx-introspection nutzt CPM.cmake, das sich beim Configure selbst per
# file(DOWNLOAD) aus dem Internet nachlädt -- das scheitert in der
# netzwerk-sandboxed do_configure-Task. Datei stattdessen vorab per
# SRC_URI holen (do_fetch ist erlaubt) und an die von CPM erwartete
# Stelle legen, damit der Hash-Check greift und der Download übersprungen wird.

CPM_SOURCE_CACHE = "${WORKDIR}/cpm-cache"

SRC_URI += "https://github.com/cpm-cmake/CPM.cmake/releases/download/v0.40.0/CPM.cmake;name=cpmcmake;downloadfilename=CPM.cmake;subdir=cpm-download"
SRC_URI[cpmcmake.sha256sum] = "7b354f3a5976c4626c876850c93944e52c83ec59a159ae5de5be7983f0e17a2a"

EXTRA_OECMAKE += "-DCPM_SOURCE_CACHE=${CPM_SOURCE_CACHE}"

do_configure:prepend() {
    mkdir -p ${CPM_SOURCE_CACHE}/cpm
    cp ${WORKDIR}/cpm-download/CPM.cmake ${CPM_SOURCE_CACHE}/cpm/CPM_0.40.0.cmake
}