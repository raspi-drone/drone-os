# The upstream meta-ros patch is stale relative to the currently fetched
# foxglove_bridge source (hunks 2–7 fail to apply).
SRC_URI:remove = "file://disable-compiler-options.patch"

# foxglove_bridge uses CMake FetchContent during configuration to download
# a prebuilt Foxglove C++ SDK binary from GitHub, which fails in the
# network-sandboxed do_configure task. Fetch the SDK via SRC_URI instead
# (BitBake automatically extracts .zip archives during do_unpack) and
# redirect FetchContent to the local copy using FETCHCONTENT_SOURCE_DIR.

FOXGLOVE_SDK_VERSION = "0.19.0"
FOXGLOVE_SDK_PLATFORM = "aarch64-unknown-linux-gnu"
FOXGLOVE_SDK_SHA = "7b1a7911f4cdf491ea6d38486af6f730ab3c90d4e54071a85bb887994424e51b"

SRC_URI += "https://github.com/foxglove/foxglove-sdk/releases/download/sdk%2Fv${FOXGLOVE_SDK_VERSION}/foxglove-v${FOXGLOVE_SDK_VERSION}-cpp-${FOXGLOVE_SDK_PLATFORM}.zip;name=foxglovesdk;downloadfilename=foxglove-sdk.zip;subdir=foxglove-sdk-extracted"
SRC_URI[foxglovesdk.sha256sum] = "${FOXGLOVE_SDK_SHA}"

FOXGLOVE_SDK_EXTRACT_DIR = "${WORKDIR}/foxglove-sdk-extracted/foxglove"

EXTRA_OECMAKE += "-DFETCHCONTENT_SOURCE_DIR_FOXGLOVE_SDK=${FOXGLOVE_SDK_EXTRACT_DIR} -DFETCHCONTENT_FULLY_DISCONNECTED=ON"

# -Werror promotes warnings originating from third-party vendored headers
# (e.g. rosx_introspection/contrib/SmallVector.h) to hard build failures.
# Remove it locally.
do_configure:prepend() {
    sed -i 's/-Werror //g' ${S}/CMakeLists.txt
}

INSANE_SKIP:${PN}-dbg += "buildpaths"