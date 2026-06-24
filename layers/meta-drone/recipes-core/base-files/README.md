# recipes-core/base-files

Custom `fstab` for Drone OS.

## Overview

Overrides the default `base-files` fstab to define the Drone OS partition
layout. The `meta-drone` layer has priority 10, ensuring this fstab takes
precedence over the one shipped by `meta-rauc-raspberrypi` (priority 6).

## Files

| File | Description |
|------|-------------|
| `base-files_%.bbappend` | Adds custom fstab to `base-files` |
| `files/fstab` | Drone OS partition mount table |

## fstab

```
LABEL=boot      /boot   vfat    defaults,rw         0  2
/dev/mmcblk0p2  /       ext4    defaults,noatime    0  1
LABEL=config    /config ext4    defaults,noatime    0  2
LABEL=data      /data   ext4    defaults,noatime    0  2
```

### Design decisions

- **`/boot` is `rw`** — U-Boot stores its environment in `/boot/uboot.env`.
  RAUC writes to this file via `fw_setenv` when marking a slot good. A
  read-only `/boot` causes `rauc-mark-good.service` to fail.

- **`/` uses device path, not label** — RAUC writes OS updates via `dd`,
  which overwrites the ext4 superblock and therefore the partition label.
  Using `LABEL=rootfs-A` for `/` would break the mount after an OS update.
  `/dev/mmcblk0p2` is always the active rootfs slot (U-Boot boots from p2 or
  p3 but the kernel command line always references p2 — slot switching is
  handled by U-Boot copying the correct slot).

- **`rootfs-B` (p3) is absent** — this slot is exclusively managed by RAUC
  and must not appear in fstab.

- **`/config` and `/data` use labels** — these partitions are never
  overwritten by RAUC in a way that loses the label (the config slot uses a
  custom hook that calls `mkfs.ext4 -L config`).

## Related

- `conf/layer.conf` — sets `BBFILE_PRIORITY_meta-drone = "10"`
- `recipes-images/images/drone-image.bb` — creates `/config` and `/data`
  mountpoint directories in the rootfs
- `wic/drone-image.wks.in` — partition layout that assigns these labels
