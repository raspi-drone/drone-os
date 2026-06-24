# recipes-bsp/uboot

U-Boot bbappend for Raspberry Pi 5.

## Overview

Configures U-Boot as the bootloader for `raspberrypi5`. Required by RAUC to
read and write boot slot selection via `fw_printenv` / `fw_setenv`.

## Files

| File | Description |
|------|-------------|
| `u-boot_%.bbappend` | U-Boot configuration for RPi5 |

## Configuration

```bitbake
COMPATIBLE_MACHINE:raspberrypi5 = "raspberrypi5"
UBOOT_MACHINE:raspberrypi5     = "rpi_arm64_defconfig"
UBOOT_ENV_SIZE:raspberrypi5    = "0x4000"
UBOOT_ENV_OFFSET:raspberrypi5  = "0x400000"
```

- `rpi_arm64_defconfig` — standard 64-bit RPi defconfig, supports RPi5
- `UBOOT_ENV_SIZE` / `UBOOT_ENV_OFFSET` — location of the U-Boot environment
  in the FAT boot partition (`/boot/uboot.env`). RAUC uses this to persist
  `BOOT_ORDER`, `BOOT_A_LEFT`, `BOOT_B_LEFT` across reboots.

## Bootchain

```
RPi5 ROM → RPi Firmware (config.txt: kernel=u-boot.bin) → U-Boot → Kernel
```

`RPI_USE_U_BOOT = "1"` in `drone-os.conf` instructs `meta-raspberrypi` to
write `kernel=u-boot.bin` into `config.txt` automatically.

## Related

- `conf/distro/drone-os.conf` — sets `RPI_USE_U_BOOT` and bootloader provider
- `recipes-core/rauc/` — RAUC system config referencing `bootloader=uboot`
- `drone-image.bb` — installs `u-boot-fw-utils` (`fw_printenv`/`fw_setenv`)
