# Drone Firewall

This layer customizes the `nftables` package by installing a predefined firewall configuration and a custom systemd service.

The firewall follows a **default-deny** approach and only allows access to selected services through the WireGuard VPN interface (`wg0`).

## Overview

The package provides:

* A managed `nftables.conf`
* A custom `nftables.service`
* Automatic firewall activation during boot

The firewall configuration is deployed through a `bbappend` to the upstream `nftables` recipe.

## Security Model

The firewall uses the following principles:

* All incoming traffic is denied by default.
* Local loopback traffic is always allowed.
* Established and related connections are allowed.
* Invalid packets are dropped.
* SSH access is restricted to the WireGuard VPN.
* ROS2 / DDS communication is restricted to trusted VPN networks.
* Foxglove access is restricted to the WireGuard VPN.
* Packet forwarding is disabled.
* Outbound traffic is unrestricted.

This ensures that the drone remains inaccessible from external networks unless connected through the VPN.

## Installation

Add the layer containing this bbappend and include `nftables` in the image:

```bitbake
IMAGE_INSTALL:append = " nftables"
```

The layer automatically installs the custom configuration when the `nftables` package is built.

## Installed Files

### Firewall Rules

```text
/etc/nftables.conf
```

### Systemd Service

```text
/lib/systemd/system/nftables.service
```

## Firewall Rules

### Allowed Traffic

#### Loopback

```text
lo
```

All local inter-process communication is allowed.

#### Established Connections

Connection tracking allows return traffic for already established connections.

#### ICMP / ICMPv6

Basic network diagnostics and IPv6 neighbor discovery are allowed:

* ICMP echo requests (ping)
* Destination unreachable
* Time exceeded
* IPv6 neighbor solicitation
* IPv6 neighbor advertisement

#### SSH

SSH is only accessible through the WireGuard interface:

```text
Interface: wg0
Port: 22/TCP
```

#### ROS2 / DDS

ROS2 discovery and communication traffic is allowed from trusted VPN networks:

```text
Interface: wg0
Source Networks:
  - 10.0.0.0/24
  - 10.0.40.0/24

Ports:
  UDP 7400-7800
```

#### Foxglove

Foxglove WebSocket connections are allowed through the VPN:

```text
Interface: wg0
Port: 8765/TCP
```

### Blocked Traffic

All other inbound traffic is denied.

Packet forwarding is also disabled:

```text
policy drop
```

for the `forward` chain.

## Service Behavior

The custom systemd service:

```ini
[Service]
Type=oneshot
ExecStart=/usr/sbin/nft -f /etc/nftables.conf
ExecReload=/usr/sbin/nft -f /etc/nftables.conf
ExecStop=/usr/sbin/nft flush ruleset
RemainAfterExit=yes
```

loads the firewall rules during boot and reloads them when requested.

The service starts before network interfaces become active:

```ini
Before=network-pre.target
```

This minimizes the risk of services becoming reachable before firewall rules are applied.

## Verification

Check that the service is active:

```bash
systemctl status nftables
```

View the currently loaded ruleset:

```bash
nft list ruleset
```

Verify that only the expected ports are exposed through the VPN:

```bash
ss -tulpen
```

## Updating Firewall Rules

The firewall configuration is managed in:

```text
layers/meta-drone/recipes-network/drone-firewall/files/nftables.conf
```

After modifying the rules:

1. Rebuild the image.
2. Deploy the updated image to the device.
3. Verify the resulting ruleset with:

```bash
nft list ruleset
```

Because `/etc/nftables.conf` is installed from the Yocto build, firewall changes should be made in the layer rather than directly on the target system.

## Directory Structure

```text
recipes-network/drone-firewall/
├── files
│   ├── nftables.conf
│   └── nftables.service
└── nftables_%.bbappend
```
