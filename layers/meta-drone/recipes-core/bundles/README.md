# recipes-core/bundles

RAUC update bundles for Drone OS.

## Overview

Three bundle types are provided, each targeting a different update scenario:

| Recipe | Output | Contents | Use case |
|--------|--------|----------|----------|
| `drone-config-bundle.bb` | `drone-config-bundle-raspberrypi5.raucb` | Config files only (~6 KB) | OTA config update |
| `drone-os-bundle.bb` | `drone-os-bundle-raspberrypi5.raucb` | rootfs.ext4 (~190 MB) | OTA OS update |
| `drone-full-bundle.bb` | `drone-full-bundle-raspberrypi5.raucb` | rootfs.ext4 + config files | Initial OTA deploy |

All bundles are signed with `signing.cert.pem` which is issued by the same CA
(`ca.cert.pem`) that is installed in the rootfs under `/etc/rauc/ca.cert.pem`.

## Files

```
bundles/
├── drone-config-bundle.bb
├── drone-os-bundle.bb
├── drone-full-bundle.bb
└── files/
    ├── ca.cert.pem          ← CA certificate (safe to commit)
    ├── ca.cert.srl          ← CA serial (safe to commit)
    ├── signing.cert.pem     ← Signing certificate (safe to commit)
    ├── signing.key.pem      ← Signing private key (DO NOT COMMIT)
    ├── signing.csr.pem      ← CSR (DO NOT COMMIT)
    ├── wireguard/
    │   └── wg0.nmconnection
    ├── modem/
    │   └── mm-apn.nmconnection
    └── ros/
        └── mavros_params.yaml
```

## PKI Setup

The signing key and certificate must be created once and stored outside the
repository. The certificate must be signed by the same CA installed in the
rootfs.

```bash
# 1. Generate signing key and CSR
openssl req -newkey rsa:4096 \
    -keyout layers/meta-drone/recipes-core/bundles/files/signing.key.pem \
    -out    layers/meta-drone/recipes-core/bundles/files/signing.csr.pem \
    -nodes -subj "/CN=Drone OS Signing"

# 2. Sign with the RAUC CA (same CA as in recipes-core/rauc/files/)
openssl x509 -req \
    -in  layers/meta-drone/recipes-core/bundles/files/signing.csr.pem \
    -CA  layers/meta-drone/recipes-core/rauc/files/ca.cert.pem \
    -CAkey layers/meta-drone/recipes-core/rauc/files/ca.key.pem \
    -CAcreateserial \
    -out layers/meta-drone/recipes-core/bundles/files/signing.cert.pem \
    -days 3650

# 3. Copy CA cert for rauc info
cp layers/meta-drone/recipes-core/rauc/files/ca.cert.pem \
   layers/meta-drone/recipes-core/bundles/files/ca.cert.pem
```

**Important:** `signing.key.pem` and `signing.csr.pem` are listed in
`.gitignore` and must never be committed. Store them in a secrets manager or
encrypted vault.

## Config Bundle

The config bundle does **not** contain an ext4 image. Instead, the config
files are packaged as a plain directory inside the bundle. A custom
`slot-install` hook handles unmounting `/config`, formatting the partition,
copying the files, and remounting.

This approach is required because the config slot has `allow-mounted=true` in
`system.conf`, which mandates a custom install hook.

```
bundle-input/
├── manifest.raucm
├── hook.sh           ← custom install hook
└── config-root/
    ├── wireguard/wg0.nmconnection
    ├── modem/mm-apn.nmconnection
    └── ros/mavros_params.yaml
```

The hook uses `$RAUC_BUNDLE_MOUNT_POINT` to locate the bundle content and
`$RAUC_SLOT_DEVICE` for the target partition.

## OS Bundle

The OS bundle wraps the `drone-image-raspberrypi5.rootfs.ext4` produced by
`drone-image`. A `slot-post-install` hook restores the `rootfs-A` ext4 label
after `dd` overwrites the superblock.

The bundle recipe declares:
```bitbake
do_compile[depends] += "drone-image:do_image_complete"
```
This ensures the OS image is always built before the bundle.

## Full Bundle

Combines both: writes `rootfs.ext4` to the inactive rootfs slot and applies
the config files to the config slot in a single atomic RAUC operation.

## Building

```bash
# Build all bundles (via kas target list)
kas build kas/dev.yml

# Build a specific bundle only
kas shell kas/dev.yml -c "bitbake drone-config-bundle"
kas shell kas/dev.yml -c "bitbake drone-os-bundle"
kas shell kas/dev.yml -c "bitbake drone-full-bundle"

# Force rebuild
kas shell kas/dev.yml -c "bitbake drone-config-bundle -c cleansstate && bitbake drone-config-bundle"
```

## Installing

```bash
# Copy bundle to drone
scp build/tmp/deploy/images/raspberrypi5/drone-config-bundle-raspberrypi5.raucb \
    root@<IP>:/tmp/

# Install on drone
rauc install /tmp/drone-config-bundle-raspberrypi5.raucb

# Verify
rauc status
```

OS bundle requires a reboot after install. Config bundle does not.
