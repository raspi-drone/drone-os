# Networking Recipes

This directory contains all networking-related Yocto recipes used by the drone platform.

The recipes provide:

* Cellular modem connectivity
* WiFi connectivity as a fallback data path
* WireGuard VPN access
* Host firewall configuration

Together, these components establish a secure networking stack that allows remote access to the drone while minimizing exposed services.

## Overview

```text
recipes-network/
├── drone-firewall/
├── drone-modem/
├── drone-wifi/
└── drone-wireguard/
```

| Recipe            | Purpose                                                                  |
| ----------------- | ------------------------------------------------------------------------ |
| `drone-modem`     | Configures cellular connectivity through ModemManager and NetworkManager |
| `drone-wifi`      | Configures a low-priority WiFi fallback connection for when 5G is unavailable |
| `drone-wireguard` | Establishes a WireGuard VPN connection once either uplink is online      |
| `drone-firewall`  | Restricts network access to approved services over the VPN               |

## Network Architecture

The networking stack is designed around the following flow:

```text
+-------------------+     +-------------------+
| Cellular Network  |     | WiFi Network      |
+---------+---------+     +---------+---------+
          |                         |
          v                         v
+-------------------+     +-------------------+
| drone-modem       |     | drone-wifi        |
| (wwan0)           |     | (wlan0)           |
| route-metric 100  |     | route-metric 700  |
+---------+---------+     +---------+---------+
          |     preferred           | fallback
          +------------+------------+
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

1. The cellular modem and WiFi both attempt to establish Internet connectivity; the modem's lower `route-metric` makes it the preferred default route whenever it is available, with WiFi used only when the modem has no route (no SIM, no coverage, or hardware not connected).
2. WireGuard creates a secure VPN tunnel on top of whichever uplink currently has the default route.
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

### drone-wifi

Provides a low-priority WiFi fallback connection.

Responsibilities:

* Configure a WiFi connection profile for NetworkManager.
* Enable automatic WiFi connection on boot.
* Stay subordinate to the modem via a higher `route-metric`, so it only carries traffic when the modem is unavailable.

Primary interface:

```text
wlan0
```

### drone-wireguard

Provides secure remote connectivity.

Responsibilities:

* Install a WireGuard NetworkManager connection.
* Wait for the modem or WiFi connection to become available.
* Automatically establish the VPN tunnel.

Primary interface:

```text
wg0
```

The VPN tunnel is intentionally started only after an uplink (cellular or WiFi) connectivity has been established.

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
wwan0 connected ──────┐  (falls back to)
      │                wlan0 connected
      ▼                     │
      └───────┬─────────────┘
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
    drone-wifi \
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

### WiFi Fallback

```bash
nmcli connection show --active
ip route show default
```

`drone-wifi` should be active whenever WiFi is in range, but the default route (lowest `metric`) should point at `wwan0` unless the modem is unavailable.

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

### WiFi Is Used Instead Of 5G

Check the route metrics — the modem's route must have a lower `metric` than WiFi's:

```bash
ip route show default
nmcli -f GENERAL.STATE,IP4.ROUTE device show wwan0
nmcli -f GENERAL.STATE,IP4.ROUTE device show wlan0
```

Confirm `mm-apn.nmconnection` has `route-metric=100` and `wifi.nmconnection` has `route-metric=700`.

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
