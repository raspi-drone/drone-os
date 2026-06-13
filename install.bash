#!/usr/bin/env bash
set -euo pipefail

echo "Updating package index..."
sudo apt update

echo "Installing Yocto/KAS host dependencies..."

sudo apt install -y \
    build-essential \
    gcc \
    g++ \
    make \
    binutils \
    cpp \
    diffstat \
    chrpath \
    file \
    gawk \
    wget \
    cpio \
    unzip \
    bzip2 \
    zstd \
    lz4 \
    liblz4-tool \
    rpcsvc-proto \
    python3 \
    python3-pip \
    gawk wget git diffstat unzip texinfo \
    gcc g++ build-essential \
    chrpath socat cpio \
    python3 python3-pip python3-pexpect \
    python3-git python3-jinja2 \
    xz-utils debianutils iputils-ping \
    libsdl1.2-dev libegl1 \
    pylint xterm \
    zstd lz4 liblz4-tool \
    file locales gfortran \
    rpcsvc-proto \
    bzip2 \
    make \
    diffutils \
    binutils \
    coreutils

echo "Verifying key tools..."

for tool in ar as bunzip2 bzip2 chrpath cpio cpp diffstat file g++ gawk gcc ld lz4c make nm objcopy objdump pzstd ranlib readelf rpcgen strings strip unzstd wget zstd; do
    if ! command -v $tool >/dev/null 2>&1; then
        echo "WARNING: $tool still missing from PATH"
    else
        echo "OK: $tool"
    fi
done

echo "Done."
