# Drone DDS Config

This layer provides the CycloneDDS runtime configuration for ROS 2 nodes running on the drone platform.

It consists of:

* A CycloneDDS XML configuration file enabling unicast peer discovery over loopback

## Overview

The package installs:

* `/etc/ros/cyclonedds.xml`

The configuration ensures that ROS 2 nodes can discover each other on the same device without requiring multicast support on the loopback interface.

## Why This Exists

CycloneDDS uses multicast by default for participant discovery.

On this platform, the loopback interface (`lo`) does not have the `MULTICAST` flag set:

```text
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536
```

Without multicast, two ROS 2 processes running on the same device cannot discover each other, even when using `ROS_AUTOMATIC_DISCOVERY_RANGE=LOCALHOST`.

This configuration replaces multicast discovery with unicast peer discovery, explicitly targeting `localhost`.

## Configuration

The installed file `/etc/ros/cyclonedds.xml` contains:

```xml
<CycloneDDS>
  <Domain>
    <General>
      <Interfaces>
        <NetworkInterface name="lo"/>
      </Interfaces>
    </General>
    <Discovery>
      <Peers>
        <Peer Address="localhost"/>
      </Peers>
      <ParticipantIndex>auto</ParticipantIndex>
    </Discovery>
  </Domain>
</CycloneDDS>
```

This restricts DDS traffic to the loopback interface and uses unicast for all participant discovery.

## Integration

The configuration is activated by setting:

```bash
export CYCLONEDDS_URI=file:///etc/ros/cyclonedds.xml
```

This variable is exported automatically by `/etc/profile.d/ros2.sh` (provided by `ros-env`) and is set in the `foxglove-bridge.service` unit.

No manual setup is required after flashing the image.

## Installation

Add the package to the image:

```bitbake
IMAGE_INSTALL:append = " drone-dds-config"
```

## Verification

After boot, verify that ROS 2 nodes can discover each other:

```bash
source /etc/profile.d/ros2.sh
ros2 node list
```

The `foxglove_bridge` node should appear in the output.

Verify that topic publishing works across process boundaries:

```bash
ros2 topic pub -r 1 /test std_msgs/msg/Int32 "{data: 1}"
```

In a second terminal, verify the topic is visible:

```bash
ros2 topic list
```

## Directory Structure

```text
recipes-ros/drone-dds-config/
├── drone-dds-config_0.1.bb
└── files
    └── cyclonedds.xml
```

## Notes for Maintainers

This configuration intentionally limits DDS discovery to the loopback interface.

No DDS traffic is exchanged over the WireGuard (`wg0`) or cellular (`wwan0`) interfaces.

Remote visualization is handled exclusively by Foxglove Bridge over WebSocket (TCP 8765), not by DDS.

If the kernel configuration changes and `lo` gains multicast support, this explicit configuration can be removed and replaced with `ROS_AUTOMATIC_DISCOVERY_RANGE=LOCALHOST` alone.

When updating CycloneDDS, verify that:

1. The XML schema for `<Interfaces>` and `<Discovery>` has not changed.
2. The `ParticipantIndex=auto` setting is still supported.
3. Unicast peer discovery still takes precedence over multicast when both are configured.