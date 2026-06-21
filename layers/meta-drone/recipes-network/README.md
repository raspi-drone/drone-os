# Networking Recipes

This directory contains all networking-related Yocto recipes used by the drone platform.

The recipes provide:

* Cellular modem connectivity
* WireGuard VPN access
* Host firewall configuration

Together, these components establish a secure networking stack that allows remote access to the drone while minimizing exposed services.

## Overview

```text
recipes-network/
├── drone-firewall/
├── drone-modem/
└── drone-wireguard/
```

| Recipe            | Purpose                                                                  |
| ----------------- | ------------------------------------------------------------------------ |
| `drone-modem`     | Configures cellular connectivity through ModemManager and NetworkManager |
| `drone-wireguard` | Establishes a WireGuard VPN connection once the modem is online          |
| `drone-firewall`  | Restricts network access to approved services over the VPN               |

## Network Architecture

The networking stack is designed around the following flow:

```text
+-------------------+
| Cellular Network  |
+---------+---------+
          |
          v
+-------------------+
| drone-modem       |
| (wwan0)           |
+---------+---------+
          |
          v
+-------------------+
| drone-wireguard   |
| (wg0)             |
+---------+---------+
          |
          v
+-------------------+
| drone-firewall    |
+---------+---------+
          |
          v
+-------------------+
| ROS2 Services     |
| Foxglove          |
| SSH               |
+-------------------+
```

1. The cellular modem establishes Internet connectivity.
2. WireGuard creates a secure VPN tunnel.
3. The firewall permits access only through the VPN.
4. Application services become reachable through the secured network.

## Component Details

### drone-modem

Provides the cellular modem configuration.

Responsibilities:

* Configure a GSM/LTE connection profile for NetworkManager.
* Enable automatic cellular connection on boot.
* Ensure NetworkManager starts after ModemManager.

Primary interface:

```text
wwan0
```

### drone-wireguard

Provides secure remote connectivity.

Responsibilities:

* Install a WireGuard NetworkManager connection.
* Wait for the modem connection to become available.
* Automatically establish the VPN tunnel.

Primary interface:

```text
wg0
```

The VPN tunnel is intentionally started only after cellular connectivity has been established.

### drone-firewall

Provides host-level packet filtering using nftables.

Responsibilities:

* Enforce a default-deny inbound policy.
* Restrict SSH access to the VPN.
* Restrict ROS2 and Foxglove access to trusted networks.
* Disable packet forwarding.

The firewall is loaded before normal network startup.

## Boot Sequence

The expected startup sequence is:

```text
ModemManager
      │
      ▼
NetworkManager
      │
      ▼
wwan0 connected
      │
      ▼
WireGuard dispatcher
      │
      ▼
wg0 connected
      │
      ▼
Remote access available
```

Firewall rules are already active during this process.

## Exposed Services

The following services are expected to be reachable through the WireGuard VPN:

| Service         | Protocol | Port      |
| --------------- | -------- | --------- |
| SSH             | TCP      | 22        |
| ROS2 / DDS      | UDP      | 7400–7800 |
| Foxglove Bridge | TCP      | 8765      |

No other inbound services should be accessible.

## Security Model

The drone networking stack follows several security principles:

### VPN-Only Access

Administrative and operational services are only reachable through the WireGuard tunnel.

### Default-Deny Firewall

All incoming traffic is blocked unless explicitly permitted.

### No Routing

Packet forwarding is disabled to prevent the drone from acting as a gateway.

### Minimal Exposure

Only the ports required for:

* SSH
* ROS2
* Foxglove

are exposed.

## Installation

Typical image configuration:

```bitbake
IMAGE_INSTALL:append = " \
    drone-modem \
    drone-wireguard \
    nftables \
"
```

Depending on the image structure, `drone-firewall` is automatically applied through the `nftables` bbappend.

## Verification

After boot, verify the networking stack:

### Modem

```bash
nmcli connection show --active
```

Expected:

```text
drone-modem
```

### WireGuard

```bash
nmcli connection show --active
```

Expected:

```text
wg0
```

### Firewall

```bash
nft list ruleset
```

### Listening Services

```bash
ss -tulpen
```

### Connectivity

```bash
ping <vpn-peer>
```

## Troubleshooting

### Modem Does Not Connect

Check:

```bash
mmcli -L
journalctl -u ModemManager
```

### WireGuard Does Not Start

Check:

```bash
journalctl -t wireguard-dispatcher
cat /tmp/nm-dispatch.log
```

Verify:

```bash
nmcli connection show wg0
```

### Firewall Blocks Traffic

Inspect:

```bash
nft list ruleset
```

Confirm that the source IP is within an allowed VPN network.

## Design Goals

The networking stack is designed to provide:

* Reliable cellular connectivity
* Secure remote access
* Minimal exposed attack surface
* Reproducible deployment through Yocto
* Clear separation between modem, VPN, and firewall responsibilities
