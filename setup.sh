#!/bin/bash
# setup.sh — Initializes the Yocto build environment for Raspberry Pi CM5
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
YOCTO_RELEASE="scarthgap"
BUILD_DIR="${SCRIPT_DIR}/build"

echo "==> Setting up Yocto (${YOCTO_RELEASE}) for Raspberry Pi CM5"

# --- Clone layers -----------------------------------------------------------

clone_if_missing() {
    local name="$1" url="$2" branch="$3"
    if [ ! -d "${SCRIPT_DIR}/${name}" ]; then
        echo "  Cloning ${name}..."
        git clone --depth=1 --branch "${branch}" "${url}" "${SCRIPT_DIR}/${name}"
    else
        echo "  ${name} already present, skipping."
    fi
}

clone_if_missing poky \
    "https://git.yoctoproject.org/poky" \
    "${YOCTO_RELEASE}"

clone_if_missing meta-openembedded \
    "https://github.com/openembedded/meta-openembedded.git" \
    "${YOCTO_RELEASE}"

clone_if_missing meta-raspberrypi \
    "https://github.com/agherzan/meta-raspberrypi.git" \
    "${YOCTO_RELEASE}"

# --- Initialize build environment -------------------------------------------

echo "==> Initializing build directory: ${BUILD_DIR}"
# oe-init-build-env must be sourced; disable -u beforehand because the script
# references BBSERVER before it is set, which triggers an "unbound variable"
# error under set -u.
(
    set +u
    # shellcheck source=/dev/null
    source "${SCRIPT_DIR}/poky/oe-init-build-env" "${BUILD_DIR}" > /dev/null
)

# Always overwrite bblayers.conf (oe-init-build-env generates a minimal
# version on first run; we need to replace it with our full layer stack).
# For local.conf only skip if it already exists to preserve user edits.
echo "  Installing conf/bblayers.conf"
cp "${SCRIPT_DIR}/conf/bblayers.conf" "${BUILD_DIR}/conf/bblayers.conf"

if [ ! -f "${BUILD_DIR}/conf/local.conf" ]; then
    echo "  Installing conf/local.conf"
    cp "${SCRIPT_DIR}/conf/local.conf" "${BUILD_DIR}/conf/local.conf"
else
    echo "  conf/local.conf already exists — skipping (delete to reset)"
fi

echo ""
echo "==> Done! To start a build:"
echo ""
echo "    source poky/oe-init-build-env build"
echo "    bitbake raspi-drone-image"
echo ""
