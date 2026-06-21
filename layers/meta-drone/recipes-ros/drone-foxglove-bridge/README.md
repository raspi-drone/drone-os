# Drone Foxglove-bridge

This layer provides a managed deployment of Foxglove Bridge for ROS 2 systems running on the drone platform.

It consists of:

* Build fixes for upstream ROS packages (`foxglove-bridge` and `rosx-introspection`)
* A systemd service for automatically starting Foxglove Bridge
* A predefined runtime configuration for ROS 2 networking

## Overview

The package installs:

* `foxglove-bridge.service`
* Build-time patches via Yocto `bbappend` files for:

  * `foxglove-bridge`
  * `rosx-introspection`

The service automatically starts Foxglove Bridge during boot and exposes a WebSocket endpoint for remote visualization and debugging.

## Installation

Add the package to the image:

```bitbake
IMAGE_INSTALL:append = " drone-foxglove-bridge"
```

The package automatically pulls in:

```text
foxglove-bridge
```

through a runtime dependency.

## Why the bbappends Exist

### foxglove-bridge

The upstream recipe relies on CMake `FetchContent` to download the Foxglove C++ SDK during configuration.

In a Yocto build this fails because:

* Network access is only permitted during `do_fetch`
* `do_configure` runs in a network-sandboxed environment

The bbappend solves this by:

1. Downloading the SDK via `SRC_URI`
2. Verifying the archive checksum
3. Extracting it during `do_unpack`
4. Redirecting `FetchContent` to the local copy

This makes the build fully reproducible and compatible with Yocto's fetch model.

### rosx-introspection

`rosx-introspection` uses `CPM.cmake`, which attempts to download itself during CMake configuration using `file(DOWNLOAD)`.

This also fails in Yocto's sandboxed build environment.

The bbappend solves this by:

1. Downloading `CPM.cmake` during `do_fetch`
2. Storing it in a local cache
3. Pointing CMake at the cached version

As a result, no network access is required during configuration.

### Compiler Warning Fix

The upstream build enables:

```text
-Werror
```

which converts warnings originating from third-party vendored code into build failures.

The bbappend removes this flag to prevent failures caused by external dependencies that are not under project control.

## Systemd Service

The package installs:

```text
/lib/systemd/system/foxglove-bridge.service
```

and enables it automatically during boot.

### Service Configuration

The service launches:

```bash
ros2 launch foxglove_bridge foxglove_bridge_launch.xml port:=8765
```

with the following environment:

| Variable                      | Value              |
| ----------------------------- | ------------------ |
| ROS_DOMAIN_ID                 | 42                 |
| RMW_IMPLEMENTATION            | rmw_cyclonedds_cpp |
| ROS_AUTOMATIC_DISCOVERY_RANGE | LOCALHOST          |
| ROS_LOG_DIR                   | /tmp/ros-log       |
| HOME                          | /root              |

### Networking

Foxglove Bridge listens on:

```text
TCP 8765
```

The service itself does not enforce access control.

Access should be restricted through the system firewall and WireGuard configuration.

## Service Management

Check service status:

```bash
systemctl status foxglove-bridge
```

View logs:

```bash
journalctl -u foxglove-bridge -f
```

Restart the service:

```bash
systemctl restart foxglove-bridge
```

## Connecting from Foxglove

After establishing the VPN connection to the drone, connect using:

```text
ws://<drone-ip>:8765
```

Example:

```text
ws://10.0.50.2:8765
```

The exact address depends on the deployed WireGuard configuration.

## Verification

Verify that the service is running:

```bash
systemctl is-active foxglove-bridge
```

Verify that the port is listening:

```bash
ss -tlnp | grep 8765
```

Verify that ROS nodes are visible:

```bash
source /etc/profile.d/ros2.sh
ros2 node list
```

## Directory Structure

```text
recipes-ros/drone-foxglove-bridge/
├── drone-foxglove-bridge_0.1.bb
├── files
│   └── foxglove-bridge.service
├── foxglove-bridge_%.bbappend
└── rosx-introspection_%.bbappend
```

## Notes for Maintainers

The custom bbappends intentionally remove all build-time network dependencies from the affected upstream packages.

When updating either:

* `foxglove-bridge`
* `foxglove-sdk`
* `rosx-introspection`
* `CPM.cmake`

verify that:

1. The upstream build system has not changed.
2. Download URLs remain valid.
3. SHA256 checksums are updated.
4. The local-cache mechanism is still respected by CMake.

Failing to do so may reintroduce network access during `do_configure`, causing Yocto builds to fail.
