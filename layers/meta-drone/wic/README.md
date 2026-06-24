# wic

Disk image partition layout for Drone OS.

## Overview

Defines the GPT partition table written to the SD card or eMMC during initial
flash. The layout provides five partitions: boot, two redundant rootfs slots
for A/B updates, a config partition, and a data partition.

## Files

| File | Description |
|------|-------------|
| `drone-image.wks.in` | Wic kickstart file for `drone-image` |

## Partition Layout

```
┌─────────────────────────────────────────────────────┐
│  p1  boot      FAT32   256 MB   /boot               │
│       RPi Firmware, U-Boot, Kernel, DTBs            │
├─────────────────────────────────────────────────────┤
│  p2  rootfs-A  ext4    4 GB     /                   │
│       Active OS slot                                 │
├─────────────────────────────────────────────────────┤
│  p3  rootfs-B  ext4    4 GB     (unmounted)         │
│       Inactive OS slot — RAUC update target         │
├─────────────────────────────────────────────────────┤
│  p4  config    ext4    128 MB   /config             │
│       Device-specific config overrides              │
├─────────────────────────────────────────────────────┤
│  p5  data      ext4    128 MB*  /data               │
│       Logs, ROS bagfiles, persistent data           │
└─────────────────────────────────────────────────────┘
```

*On a 32 GB eMMC, `/data` should be increased to ~23 GB.

## drone-image.wks.in

```wks
bootloader --ptable gpt

part /boot --source bootimg-partition --ondisk mmcblk0 --fstype=vfat --label boot --active --fixed-size 256M
part / --source rootfs --ondisk mmcblk0 --fstype=ext4 --label rootfs-A --fixed-size 4G
part --ondisk mmcblk0 --fstype=ext4 --label rootfs-B --fixed-size 4G
part /config --ondisk mmcblk0 --fstype=ext4 --label config --fixed-size 128M
part /data --ondisk mmcblk0 --fstype=ext4 --label data --fixed-size 128M
```

### Design decisions

**GPT is required.** MBR supports at most 4 primary partitions. With 5
partitions, `bootloader --ptable gpt` is mandatory. Without it, the 5th
partition ends up inside an extended partition, which breaks the partition
numbering that RAUC and the fstab depend on.

**Each `part` entry must be on a single line.** The wic kickstart parser does
not support backslash line continuation. Multi-line entries cause a
`ValueError: No escaped character` parse error.

**`--fixed-size` is required for all partitions.** Using `--align` without a
size results in a zero-size partition error during `do_image_wic`.

**`rootfs-B` has no mountpoint.** The inactive rootfs slot is never mounted by
the OS. RAUC mounts it internally during updates.

**Labels are used for config and data.** The config slot hook calls
`mkfs.ext4 -L config` to preserve the label after formatting. The data
partition is never reformatted. Both are safe to address by label in fstab.

**`/` is not addressed by label in fstab.** RAUC writes OS updates via `dd`,
which overwrites the ext4 superblock including the label. The fstab uses
`/dev/mmcblk0p2` directly to avoid mount failures after an OS update.

## Activation

The WKS file is referenced from `conf/distro/drone-os.conf`:

```bitbake
WKS_FILE = "drone-image.wks.in"
IMAGE_FSTYPES:append = " ext4 wic wic.bz2 wic.bmap"
```

## Flashing

```bash
# Flash to SD card or eMMC (replace sdX)
bzcat build/tmp/deploy/images/raspberrypi5/drone-image-raspberrypi5.rootfs.wic.bz2 \
    | sudo dd of=/dev/sdX bs=4M status=progress conv=fsync
```

## Verifying on target

```bash
# Check partition labels
blkid | grep mmcblk0p

# Expected output:
# /dev/mmcblk0p1: LABEL="boot" TYPE="vfat"
# /dev/mmcblk0p2: TYPE="ext4"
# /dev/mmcblk0p3: TYPE="ext4"
# /dev/mmcblk0p4: LABEL="config" TYPE="ext4"
# /dev/mmcblk0p5: LABEL="data" TYPE="ext4"

# Check mounts
mount | grep mmcblk
```

## Related

- `conf/distro/drone-os.conf` — sets `WKS_FILE` and `IMAGE_FSTYPES`
- `recipes-core/base-files/files/fstab` — mount table matching this layout
- `recipes-core/rauc/files/system.conf` — RAUC slot definitions matching p2/p3/p4
