# raspi-drone OS

Yocto-based Linux image for the **Raspberry Pi CM5** (BCM2712).
Built with [BitBake](https://docs.yoctoproject.org/bitbake/) on the **scarthgap** (5.0 LTS) release.

## What's included

| Package | Source layer |
|---------|-------------|
| OpenSSH server | `poky` (`IMAGE_FEATURES`) |
| `btop` | `meta-openembedded/meta-oe` |
| `tree` | `meta-openembedded/meta-oe` |

## Prerequisites

- Linux host with the [Yocto required packages](https://docs.yoctoproject.org/ref-manual/system-requirements.html#required-packages-for-the-build-host) installed
- Git ≥ 2.x, Python 3.8+, `diffstat`, `chrpath`, `cpio`, `file`, `gawk`

```bash
# Ubuntu / Debian example
sudo apt install gawk wget git diffstat unzip texinfo gcc build-essential \
     chrpath socat cpio python3 python3-pip python3-pexpect \
     xz-utils debianutils iputils-ping python3-git python3-jinja2 \
     python3-subunit zstd liblz4-tool file locales libacl1
```

## Quick start

```bash
# 1. Clone this repo and enter the os/ directory
cd raspi-drone/os

# 2. Run the setup script — clones poky, meta-openembedded, meta-raspberrypi
#    and initialises the build directory
bash setup.sh

# 3. Enter the BitBake environment
source poky/oe-init-build-env build

# 4. Build the image (takes 1–3 h on first run)
bitbake raspi-drone-image
```

The resulting `.img` file is written to `build/tmp/deploy/images/raspberrypi-cm5/`.
Flash it with `dd` or [Raspberry Pi Imager](https://www.raspberrypi.com/software/).

## Project structure

```
os/
├── setup.sh                          # One-shot environment initialiser
├── conf/
│   ├── local.conf                    # Machine, parallelism, image settings
│   └── bblayers.conf                 # Layer stack
└── meta-raspi-drone/                 # Custom layer
    ├── conf/layer.conf
    └── recipes-core/images/
        └── raspi-drone-image.bb      # Image recipe
```

Layers cloned by `setup.sh` (not tracked in this repo):

```
os/
├── poky/                 # Yocto reference distro (scarthgap)
├── meta-openembedded/    # OE layer collection (scarthgap)
└── meta-raspberrypi/     # RPi BSP layer (scarthgap)
```

> **Note on CM5 machine name:** `raspberrypi-cm5` was added to `meta-raspberrypi`
> during the scarthgap development cycle. If your checkout does not include it yet,
> change `MACHINE` in `conf/local.conf` to `raspberrypi5` (same BCM2712 SoC).

## Customisation

- **Add packages:** append to `IMAGE_INSTALL:append` in
  [meta-raspi-drone/recipes-core/images/raspi-drone-image.bb](meta-raspi-drone/recipes-core/images/raspi-drone-image.bb).
- **Tune hardware:** edit `ENABLE_UART`, `GPU_MEM`, etc. in
  [conf/local.conf](conf/local.conf).
- **Add layers:** list them in [conf/bblayers.conf](conf/bblayers.conf) and clone them before running `setup.sh`.
