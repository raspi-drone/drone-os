# rosx-introspection uses CPM.cmake, which attempts to download itself
# from the Internet via file(DOWNLOAD) during configuration. This fails
# in the network-sandboxed do_configure task. Instead, fetch the file in
# advance via SRC_URI (do_fetch is allowed) and place it at the location
# expected by CPM so that the hash check succeeds and the download step
# is skipped.

CPM_SOURCE_CACHE = "${WORKDIR}/cpm-cache"

SRC_URI += "https://github.com/cpm-cmake/CPM.cmake/releases/download/v0.40.0/CPM.cmake;name=cpmcmake;downloadfilename=CPM.cmake;subdir=cpm-download"
SRC_URI[cpmcmake.sha256sum] = "7b354f3a5976c4626c876850c93944e52c83ec59a159ae5de5be7983f0e17a2a"

EXTRA_OECMAKE += "-DCPM_SOURCE_CACHE=${CPM_SOURCE_CACHE}"

do_configure:prepend() {
    mkdir -p ${CPM_SOURCE_CACHE}/cpm
    cp ${WORKDIR}/cpm-download/CPM.cmake ${CPM_SOURCE_CACHE}/cpm/CPM_0.40.0.cmake
}