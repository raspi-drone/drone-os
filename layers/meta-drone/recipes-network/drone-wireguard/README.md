# Drone Wireguard

This Yocto recipe installs a predefined WireGuard connection for NetworkManager and a dispatcher script that automatically brings up the VPN once the cellular modem interface (`wwan0`) becomes available.

## Overview

The package installs:

* A WireGuard NetworkManager connection profile (`wg0.nmconnection`)
* A NetworkManager dispatcher script (`90-wireguard`)

The dispatcher monitors NetworkManager events and automatically starts the WireGuard tunnel after the modem connection is established.

## Installation

Add the layer containing this recipe to your build and include the package in your image:

```bitbake
IMAGE_INSTALL:append = " drone-wireguard"
```

## NetworkManager Dispatcher

The dispatcher script is installed to:

```text
/etc/NetworkManager/dispatcher.d/90-wireguard
```

When NetworkManager reports that interface `wwan0` is in state `up`, the script:

1. Waits 20 seconds to allow the cellular connection to stabilize.
2. Checks whether `wg0` is already active.
3. Starts the WireGuard connection if necessary.

Logs can be viewed using:

```bash
journalctl -t wireguard-dispatcher
```

Additional debug output is written to:

```text
/tmp/nm-dispatch.log
```

## Creating Your Own WireGuard Configuration

The actual `wg0.nmconnection` file is intentionally not stored in the repository and should be created locally with your own credentials and peer configuration.

Create the file:

```text
layers/meta-drone/recipes-network/drone-wireguard/files/wg0.nmconnection
```

using the following template:

```ini
[connection]
id=wg0
type=wireguard
interface-name=wg0
autoconnect=false

[wireguard]
listen-port=50000
private-key=<YOUR_PRIVATE_KEY>

[wireguard-peer.<PEER_PUBLIC_KEY>]
endpoint=<VPN_SERVER_FQDN_OR_IP>:51820
persistent-keepalive=25
allowed-ips=10.0.0.0/24;10.0.40.0/24;10.0.50.0/24;

[ipv4]
address1=<CLIENT_VPN_IP>/32
method=manual
dns=9.9.9.9;
dns-search=

[ipv6]
addr-gen-mode=stable-privacy
dns-search=
method=ignore
```

### Parameter Description

| Parameter                 | Description                                |
| ------------------------- | ------------------------------------------ |
| `<YOUR_PRIVATE_KEY>`      | Private WireGuard key of the client device |
| `<PEER_PUBLIC_KEY>`       | Public WireGuard key of the VPN server     |
| `<VPN_SERVER_FQDN_OR_IP>` | Hostname or IP address of the VPN endpoint |
| `<CLIENT_VPN_IP>`         | VPN address assigned to this device        |

### File Permissions

NetworkManager requires the connection file to be readable only by root:

```bash
chmod 600 wg0.nmconnection
```

## Dependencies

The package recommends:

```text
drone-modem
```

This is a soft dependency because the dispatcher reacts to the modem interface (`wwan0`) becoming available, but the package itself can be installed independently.

## Directory Structure

```text
recipes-network/drone-wireguard/
├── drone-wireguard_0.1.bb
└── files
    ├── 90-wireguard
    └── wg0.nmconnection
```
