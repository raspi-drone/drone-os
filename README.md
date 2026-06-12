# Drone OS

Drone OS is a Yocto Project based Linux image for the Raspberry Pi 5.
The build is driven by kas and uses a custom layer, `meta-drone`, to define
the `drone-os` distro and the `drone-image` root filesystem image.

## Overview

- Machine: `raspberrypi5`
- Distro: `drone-os`
- Image target: `drone-image`
- Build system: kas + Yocto Project Scarthgap

## Repository Layout

- `kas/base.yml` defines the shared Yocto configuration, repositories, machine,
	and target.
- `kas/dev.yml` adds development-only packages and debug settings.
- `layers/meta-drone/` contains the custom distro, image recipe, and layer
	metadata.

## Prerequisites

You need a Linux host with the standard Yocto build dependencies, plus kas.
Install the exact package names recommended by your distribution and Yocto
version.

## Build

Build the development configuration with:

```bash
kas build kas/dev.yml
```

The development config enables `debug-tweaks` and installs extra tools such as
`gdb`, `strace`, `tcpdump`, and `vim`.

## Custom Layer

The `meta-drone` layer provides:

- `conf/distro/drone-os.conf` for the custom distro settings
- `recipes-images/images/drone-image.bb` for the image recipe
- `recipes-example/` for example recipes

## Notes

- `kas/base.yml` pins the build to Scarthgap branches for `poky`,
	`meta-openembedded`, and `meta-raspberrypi`.
- `meta-drone` is added from the local workspace path, so the layer is built
	directly from this repository.

If you want, I can also add a release section, flashing instructions, or a
shorter contributor-focused README variant.

