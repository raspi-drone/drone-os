# Drone WiFi

This Yocto recipe installs a WiFi NetworkManager connection profile that serves as a **low-priority fallback data path** for the cellular modem (`drone-modem`).

## Overview

The package installs:

* A NetworkManager WiFi connection profile (`wifi.nmconnection`)

WiFi and the cellular modem can be connected at the same time. Routing priority between them is controlled purely through `route-metric` — no extra daemon or script is needed to prefer one over the other:

| Interface   | Connection   | `route-metric` | Preference          |
| ----------- | ------------ | -------------- | -------------------- |
| `wwan0`     | `drone-modem`| `100`          | Preferred (5G)        |
| `wlan0`     | `drone-wifi` | `700`          | Fallback (WiFi)       |

A lower `route-metric` wins the default route. As long as the modem is attached to the network and has a default route, all outbound traffic uses `wwan0`. If the modem is unavailable (no SIM/no coverage/hardware not connected), its default route disappears and the kernel falls back to the WiFi default route automatically — no manual intervention required.

## Installation

Add the layer containing this recipe to your build and include the package in your image:

```bitbake
IMAGE_INSTALL:append = " drone-wifi"
```

The recipe pulls in `networkmanager-wifi` (and transitively `wpa-supplicant`) via `RDEPENDS`.

## Creating Your Own WiFi Configuration

`wifi.nmconnection` ships with placeholder credentials and must be edited with the real SSID/password before building:

```text
layers/meta-drone/recipes-network/drone-wifi/files/wifi.nmconnection
```

```ini
[connection]
id=drone-wifi
type=wifi
interface-name=wlan0
autoconnect=true
autoconnect-priority=-10

[wifi]
mode=infrastructure
ssid=<YOUR_SSID>

[wifi-security]
key-mgmt=wpa-psk
psk=<YOUR_PASSWORD>

[ipv4]
method=auto
route-metric=700

[ipv6]
method=auto
route-metric=700
```

### Parameter Description

| Parameter        | Description                                          |
| ---------------- | ----------------------------------------------------- |
| `<YOUR_SSID>`     | SSID of the WiFi network to connect to                |
| `<YOUR_PASSWORD>` | WPA/WPA2-PSK password of that WiFi network             |

`route-metric=700` must stay higher (numerically) than the modem's `route-metric=100` (in `drone-modem`'s `mm-apn.nmconnection`) so that WiFi is only ever used when the cellular default route is absent.

### File Permissions

NetworkManager requires connection profiles to be readable only by root:

```bash
chmod 600 wifi.nmconnection
```

## WireGuard On Top

`drone-wireguard`'s dispatcher (`90-wireguard`) brings up `wg0` whenever either `wwan0` or `wlan0` comes up, so the VPN tunnel is established regardless of which uplink is currently active.

## Verification

Check that both connections are known to NetworkManager:

```bash
nmcli connection show
```

Check which connection is currently providing the default route:

```bash
ip route show default
```

The route with the lowest metric (normally the modem, `metric 100`) is the one actually used for outbound traffic.

## Directory Structure

```text
recipes-network/drone-wifi/
├── drone-wifi_0.1.bb
└── files
    └── wifi.nmconnection
```
