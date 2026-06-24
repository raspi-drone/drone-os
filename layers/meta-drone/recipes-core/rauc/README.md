# recipes-core/rauc

RAUC system configuration and PKI for Drone OS.

## Overview

Provides the RAUC system configuration (`system.conf`) and the CA certificate
(`ca.cert.pem`) installed on the target. Together these define which slots
RAUC can update and which bundles it trusts.

## Files

```
rauc/
├── rauc-conf.bbappend
└── files/
    ├── system.conf          ← RAUC slot configuration
    ├── ca.cert.pem          ← CA certificate (installed to /etc/rauc/)
    ├── ca.cert.srl          ← CA serial (safe to commit)
    ├── ca.key.pem           ← CA private key (DO NOT COMMIT)
    ├── signing.cert.pem     ← Signing certificate (safe to commit)
    ├── signing.csr.pem      ← CSR (DO NOT COMMIT)
    └── signing.key.pem      ← Signing private key (DO NOT COMMIT)
```

## PKI Setup

Create the CA and signing certificates once. Store private keys outside the
repository.

```bash
# CA
openssl req -x509 -newkey rsa:4096 \
    -keyout layers/meta-drone/recipes-core/rauc/files/ca.key.pem \
    -out    layers/meta-drone/recipes-core/rauc/files/ca.cert.pem \
    -days 3650 -nodes -subj "/CN=Drone OS CA"

# Signing key + CSR
openssl req -newkey rsa:4096 \
    -keyout layers/meta-drone/recipes-core/rauc/files/signing.key.pem \
    -out    layers/meta-drone/recipes-core/rauc/files/signing.csr.pem \
    -nodes -subj "/CN=Drone OS Signing"

# Sign with CA
openssl x509 -req \
    -in  layers/meta-drone/recipes-core/rauc/files/signing.csr.pem \
    -CA  layers/meta-drone/recipes-core/rauc/files/ca.cert.pem \
    -CAkey layers/meta-drone/recipes-core/rauc/files/ca.key.pem \
    -CAcreateserial \
    -out layers/meta-drone/recipes-core/rauc/files/signing.cert.pem \
    -days 3650
```

**Important:** `*.key.pem` and `*.csr.pem` are listed in `.gitignore`.
Only `ca.cert.pem`, `signing.cert.pem`, and `ca.cert.srl` belong in the repo.

The bundle recipes under `recipes-core/bundles/` use their own
`signing.key.pem` which **must be signed by the same CA** as the one installed
here. Using a different CA causes `signature verification failed` errors.

## system.conf

```ini
[system]
compatible=@@MACHINE@@
bootloader=uboot
data-directory=/data/

[keyring]
path=/etc/rauc/ca.cert.pem

[slot.rootfs.0]
device=/dev/mmcblk0p2
type=ext4
bootname=A

[slot.rootfs.1]
device=/dev/mmcblk0p3
type=ext4
bootname=B

[slot.config.0]
device=/dev/mmcblk0p4
type=ext4
allow-mounted=true
```

- `@@MACHINE@@` is replaced at build time by `sed` in `rauc-conf.bbappend`
- `data-directory=/data/` — RAUC stores slot status files on the persistent
  data partition so they survive OS updates
- `allow-mounted=true` on `config.0` — the config partition is always mounted
  at `/config`. RAUC requires a custom `slot-install` hook in the bundle when
  this option is set (see `recipes-core/bundles/`)
- `bootname=A/B` — links RAUC slots to U-Boot environment variables
  `BOOT_ORDER`, `BOOT_A_LEFT`, `BOOT_B_LEFT`

## rauc-conf.bbappend

```bitbake
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append = " file://system.conf file://ca.cert.pem"

do_install:prepend() {
    sed -i "s/@@MACHINE@@/${MACHINE}/g" ${WORKDIR}/system.conf
}
```

This bbappend overrides the `system.conf` and `ca.cert.pem` shipped by
`meta-rauc-raspberrypi`. `meta-drone` has layer priority 10, ensuring our
files win.

## Verifying on target

```bash
# Check RAUC is configured correctly
rauc status

# Check installed CA
openssl x509 -in /etc/rauc/ca.cert.pem -noout -subject
# subject=CN = Drone OS CA

# Check system config
cat /etc/rauc/system.conf
```

## Related

- `conf/distro/drone-os.conf` — adds `rauc` to `DISTRO_FEATURES`
- `recipes-bsp/uboot/` — U-Boot configuration required by `bootloader=uboot`
- `recipes-core/bundles/` — bundle recipes using the same CA
