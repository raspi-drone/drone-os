# Drone OS

Drone OS is a Yocto Project based Linux image for drone hardware based on
Raspberry Pi CM5 and RPi5. The build is driven by kas and uses a custom layer,
`meta-drone`, to define the `drone-os` distro, the `drone-image` root
filesystem image, and RAUC OTA update bundles.

## Overview

- Distro: `drone-os`
- Image target: `drone-image`
- Build system: kas + Yocto Project Scarthgap
- Bootloader: U-Boot (required for RAUC A/B updates)
- Update system: RAUC

### Supported Machines

| Machine | Config | Hardware |
|---------|--------|----------|
| `drone-rpi5` | `kas/machine/rpi5.yml` | Raspberry Pi 5 (development) |
| `drone-cm5` | `kas/machine/cm5.yml` | CM5 on Kelvin carrier board |

## Repository Layout

```
kas/
├── base.yml          ← shared repos, targets, distro config (no machine)
├── machine/
│   ├── rpi5.yml      ← machine: drone-rpi5
│   └── cm5.yml       ← machine: drone-cm5
└── features/
    ├── dev.yml       ← debug-tweaks, gdb, strace, tcpdump, vim, btop
    └── release.yml   ← production settings

layers/meta-drone/
├── conf/
│   ├── distro/drone-os.conf     ← distro definition
│   ├── layer.conf
│   └── machine/
│       ├── drone-rpi5.conf      ← RPi5 machine config + overlays
│       └── drone-cm5.conf       ← CM5 machine config + overlays
├── recipes-images/images/
│   └── drone-image.bb           ← image recipe
├── recipes-core/
│   ├── bundles/                 ← RAUC bundle recipes
│   └── rauc/                    ← RAUC system config + PKI
└── wic/
    └── drone-image.wks.in       ← GPT partition layout
```

## Prerequisites

You need a Linux host with the standard Yocto build dependencies, plus kas.
Install the exact package names recommended by your distribution and Yocto
version.

### Using uv

If you want a reproducible Python environment on a fresh host, use the
committed lock file and let `uv` create the repo-local `.venv` for you:

```bash
uv sync --locked
```

After that, builds can be started with:

```bash
uv run --locked kas build kas/base.yml:kas/machine/rpi5.yml:kas/features/dev.yml
```

`uv sync --locked` will reuse the existing `.venv` if it is already present,
or create it automatically on a fresh machine.

## Build

Builds are composed from three YAML files on the command line:

```bash
kas build <base>:<machine>:<features>
```

### RPi5 Development

```bash
kas build kas/base.yml:kas/machine/rpi5.yml:kas/features/dev.yml
```

### CM5 Development

```bash
kas build kas/base.yml:kas/machine/cm5.yml:kas/features/dev.yml
```

### Release

```bash
kas build kas/base.yml:kas/machine/cm5.yml:kas/features/release.yml
```

The development config enables `debug-tweaks` and installs extra tools such as
`gdb`, `strace`, `tcpdump`, `vim`, and `btop`.

## Build Artefacts

After a successful build, the following files are available under
`build/tmp/deploy/images/<machine>/`:

| File | Description |
|------|-------------|
| `drone-image-<machine>.rootfs.wic.bz2` | Full disk image for initial flash |
| `drone-os-bundle-<machine>.raucb` | OS OTA update bundle (~190 MB) |
| `drone-config-bundle-<machine>.raucb` | Config OTA update bundle (~6 KB) |
| `drone-full-bundle-<machine>.raucb` | OS + Config bundle for initial OTA deploy |

## Flashing

Flash the initial image to SD card or eMMC:

```bash
bzcat build/tmp/deploy/images/<machine>/drone-image-<machine>.rootfs.wic.bz2 \
    | sudo dd of=/dev/sdX bs=4M status=progress conv=fsync
```

For CM5 eMMC flashing, put the module into USB boot mode (hold FRC button on
power-up) and follow the
[Raspberry Pi eMMC flashing instructions](https://www.raspberrypi.com/documentation/computers/compute-module.html#flash-compute-module-emmc).

## OTA Updates

```bash
# Copy bundle to drone
scp build/tmp/deploy/images/<machine>/drone-os-bundle-<machine>.raucb \
    root@<IP>:/tmp/

# Install on drone (OS update requires reboot)
ssh root@<IP> "rauc install /tmp/drone-os-bundle-<machine>.raucb && reboot"

# Config update (no reboot required)
scp build/tmp/deploy/images/<machine>/drone-config-bundle-<machine>.raucb \
    root@<IP>:/tmp/
ssh root@<IP> "rauc install /tmp/drone-config-bundle-<machine>.raucb"
```

## Partition Layout

| # | Label | Size | Mount | Contents |
|---|-------|------|-------|----------|
| p1 | boot | 256 MB | /boot | RPi Firmware, U-Boot, Kernel |
| p2 | rootfs-A | 4 GB | / | Active OS slot |
| p3 | rootfs-B | 4 GB | — | Inactive OS slot (RAUC target) |
| p4 | config | 128 MB | /config | Device-specific config |
| p5 | data | 128 MB* | /data | Logs, bagfiles, persistent data |

*Expand `/data` to fill available space on eMMC after initial flash.

## PKI Setup

RAUC bundles are signed with x.509 certificates. Keys are **not** stored in
this repository. Run the following once to generate them:

```bash
# CA (stored in recipes-core/rauc/files/)
openssl req -x509 -newkey rsa:4096 \
    -keyout layers/meta-drone/recipes-core/rauc/files/ca.key.pem \
    -out    layers/meta-drone/recipes-core/rauc/files/ca.cert.pem \
    -days 3650 -nodes -subj "/CN=Drone OS CA"

# Signing key (stored in recipes-core/bundles/files/)
openssl req -newkey rsa:4096 \
    -keyout layers/meta-drone/recipes-core/bundles/files/signing.key.pem \
    -out    layers/meta-drone/recipes-core/bundles/files/signing.csr.pem \
    -nodes -subj "/CN=Drone OS Signing"

openssl x509 -req \
    -in  layers/meta-drone/recipes-core/bundles/files/signing.csr.pem \
    -CA  layers/meta-drone/recipes-core/rauc/files/ca.cert.pem \
    -CAkey layers/meta-drone/recipes-core/rauc/files/ca.key.pem \
    -CAcreateserial \
    -out layers/meta-drone/recipes-core/bundles/files/signing.cert.pem \
    -days 3650

cp layers/meta-drone/recipes-core/rauc/files/ca.cert.pem \
   layers/meta-drone/recipes-core/bundles/files/ca.cert.pem
```

Private keys (`*.key.pem`, `*.csr.pem`) are listed in `.gitignore` and must
never be committed.

## Custom Layer

The `meta-drone` layer provides:

- `conf/distro/drone-os.conf` — distro definition (systemd, U-Boot, RAUC, WKS)
- `conf/machine/` — machine configs with hardware-specific overlays
- `recipes-images/images/drone-image.bb` — image recipe
- `recipes-core/bundles/` — RAUC OS, config and full bundle recipes
- `recipes-core/rauc/` — RAUC system config and CA certificate
- `recipes-core/base-files/` — custom fstab
- `recipes-bsp/uboot/` — U-Boot configuration for RPi5/CM5
- `recipes-network/` — modem, WireGuard, firewall
- `recipes-ros/` — ROS2 Jazzy, MAVROS, Foxglove Bridge, DDS config
- `wic/drone-image.wks.in` — GPT partition layout

## Notes

- `kas/base.yml` pins the build to Scarthgap branches for all upstream layers.
- `meta-drone` is loaded from the local workspace path, so the layer is built
  directly from this repository.
- The `meta-rauc-raspberrypi` community layer requires `meta-lts-mixins` on
  branch `scarthgap/u-boot` for RPi5/CM5 U-Boot support.
- `BBFILE_PRIORITY_meta-drone = "10"` ensures `meta-drone` overrides
  conflicting files from `meta-rauc-raspberrypi`.
