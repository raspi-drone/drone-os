# Drone Modem

This Yocto recipe installs the cellular modem configuration required for NetworkManager and ensures that NetworkManager starts only after ModemManager is available.

The package is intended for systems using a GSM/LTE modem managed by ModemManager and NetworkManager.

## Overview

The package installs:

* A NetworkManager GSM connection profile (`mm-apn.nmconnection`)
* A systemd drop-in for NetworkManager (`modem.conf`)

The connection profile enables automatic cellular connectivity using a configured APN, while the systemd drop-in ensures proper startup ordering between NetworkManager and ModemManager.

## Installation

Add the layer containing this recipe to your build and include the package in your image:

```bitbake
IMAGE_INSTALL:append = " drone-modem"
```

## NetworkManager Cellular Connection

The GSM connection profile is installed to:

```text
/etc/NetworkManager/system-connections/mm-apn.nmconnection
```

The profile is configured to:

* Use a GSM/LTE modem managed by ModemManager
* Automatically connect on boot
* Obtain IPv4 and IPv6 configuration automatically from the network

## Creating Your Own Modem Configuration

The actual `mm-apn.nmconnection` file may vary depending on the SIM card provider and deployment environment.

Create the file:

```text
layers/meta-drone/recipes-network/drone-modem/files/mm-apn.nmconnection
```

using the following template:

```ini
[connection]
id=drone-modem
type=gsm
autoconnect=true

[gsm]
apn=<YOUR_APN>

[ipv4]
method=auto

[ipv6]
method=auto
```

### Parameter Description

| Parameter    | Description                                  |
| ------------ | -------------------------------------------- |
| `<YOUR_APN>` | APN provided by your mobile network operator |

### Example APNs

| Provider | Example APN        |
| -------- | ------------------ |
| Swisscom | `gprs.swisscom.ch` |
| Sunrise  | `internet`         |
| Salt     | `internet`         |

Consult your mobile network provider for the correct APN settings.

### File Permissions

NetworkManager requires connection profiles to be readable only by root:

```bash
chmod 600 mm-apn.nmconnection
```

## NetworkManager Startup Dependency

The package installs the following systemd drop-in:

```text
/etc/systemd/system/NetworkManager.service.d/modem.conf
```

Contents:

```ini
[Unit]
Wants=ModemManager.service
After=ModemManager.service
```

This ensures that:

* ModemManager is started automatically when NetworkManager starts.
* NetworkManager waits until ModemManager has been started before attempting to manage modem devices.

Without this dependency, NetworkManager may start before the modem is detected, which can delay or prevent automatic cellular connectivity during boot.

## Verification

After booting the device, verify that the modem is detected:

```bash
mmcli -L
```

Check the NetworkManager connection:

```bash
nmcli connection show
```

Verify that the modem connection is active:

```bash
nmcli connection show --active
```

Check modem status:

```bash
mmcli -m 0
```

## Directory Structure

```text
recipes-network/drone-modem/
├── drone-modem_0.1.bb
└── files
    ├── mm-apn.nmconnection
    └── modem.conf
```
