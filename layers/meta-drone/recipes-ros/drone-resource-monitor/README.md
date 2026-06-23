# Drone Resource-Monitor

Yocto recipe for the [resource-monitor](https://github.com/raspi-drone/resource-monitor)
ROS 2 node. Publishes system resource metrics as `diagnostic_msgs/DiagnosticArray`
on `/diagnostics/rpi`. See the [upstream README](https://github.com/raspi-drone/resource-monitor#readme)
for details on topics, message layout and thresholds.

## Location

```
layers/meta-drone/recipes-ros/drone-resource-monitor/
├── drone-resource-monitor_0.1.bb
└── files/
    └── resource-monitor.service
```

## Enabling the recipe

Add to `drone-image.bb`:

```bitbake
IMAGE_INSTALL += "drone-resource-monitor"
```

## Service

The node is managed by systemd and starts automatically on boot.

```bash
systemctl status resource-monitor
journalctl -u resource-monitor -f
```

### Parameters

The service passes parameters via `ros2 launch`. Edit `resource-monitor.service`
to override the defaults:

| Parameter | Default | Description |
|---|---|---|
| `disk_device` | `mmcblk0` | Block device to monitor |
| `network_interface` | `wlan0` | Network interface to monitor |
| `update_rate` | `1.0` | Publish interval in seconds |

```ini
ExecStart=/bin/bash -c '\
    source /etc/profile.d/ros2.sh && \
    ros2 launch resource_monitor resource_monitor.launch.py \
        disk_device:=mmcblk0 \
        network_interface:=end0'
```

## udev

The recipe installs `/etc/udev/rules.d/99-vcio.rules` to grant access to
`/dev/vcio`, which is required for reading the RPi5 CPU throttle state via `vcgencmd`:

```
KERNEL=="vcio", MODE="0666"
```

## Known recipe quirks

`ros_ament_python` installs with prefix `/` instead of `/opt/ros/jazzy`.
`do_install:append` relocates all files to the correct paths:

| After `ros_ament_python` | Target |
|---|---|
| `/python3.12/` | `/opt/ros/jazzy/lib/python3.12/` |
| `/lib/resource_monitor/` | `/opt/ros/jazzy/lib/resource_monitor/` |
| `/ament_index/` | `/opt/ros/jazzy/share/ament_index/` |
| `/resource_monitor/` | `/opt/ros/jazzy/share/resource_monitor/` |

The shebang in the generated entry-point script is also patched — `ros_ament_python`
writes the native build-host Python path which is invalid on the target.

## Pinning SRCREV

Currently set to `${AUTOREV}` (always latest `main`). Pin to a specific commit
for reproducible builds:

```bitbake
SRCREV = "abc1234..."
```