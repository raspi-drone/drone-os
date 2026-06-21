# Upstream meta-ros patch ist stale gegenüber der aktuell gefetchten
# foxglove_bridge-Source (Hunks 2-7 schlagen fehl).
SRC_URI:remove = "file://disable-compiler-options.patch"

# foxglove_bridge lädt beim Configure per CMake FetchContent ein
# vorgebautes Foxglove-C++-SDK-Binary von GitHub -- scheitert in der
# netzwerk-sandboxed do_configure-Task. Stattdessen selbst per SRC_URI
# holen (BitBake entpackt .zip automatisch in do_unpack) und
# FetchContent per FETCHCONTENT_SOURCE_DIR auf die lokale Kopie umleiten.

FOXGLOVE_SDK_VERSION = "0.19.0"
FOXGLOVE_SDK_PLATFORM = "aarch64-unknown-linux-gnu"
FOXGLOVE_SDK_SHA = "7b1a7911f4cdf491ea6d38486af6f730ab3c90d4e54071a85bb887994424e51b"

SRC_URI += "https://github.com/foxglove/foxglove-sdk/releases/download/sdk%2Fv${FOXGLOVE_SDK_VERSION}/foxglove-v${FOXGLOVE_SDK_VERSION}-cpp-${FOXGLOVE_SDK_PLATFORM}.zip;name=foxglovesdk;downloadfilename=foxglove-sdk.zip;subdir=foxglove-sdk-extracted"
SRC_URI[foxglovesdk.sha256sum] = "${FOXGLOVE_SDK_SHA}"

FOXGLOVE_SDK_EXTRACT_DIR = "${WORKDIR}/foxglove-sdk-extracted/foxglove"

EXTRA_OECMAKE += "-DFETCHCONTENT_SOURCE_DIR_FOXGLOVE_SDK=${FOXGLOVE_SDK_EXTRACT_DIR} -DFETCHCONTENT_FULLY_DISCONNECTED=ON"

# Der entfernte Upstream-Patch hat vermutlich genau das hier erledigt:
# -Werror eskaliert Warnungen aus fremden, vendorten Headern (z.B.
# rosx_introspection/contrib/SmallVector.h) zu Hard-Fails. Selbst entfernen.
do_configure:prepend() {
    sed -i 's/-Werror //g' ${S}/CMakeLists.txt
}

INSANE_SKIP:${PN}-dbg += "buildpaths"