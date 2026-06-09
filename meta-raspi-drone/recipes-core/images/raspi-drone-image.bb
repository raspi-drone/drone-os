SUMMARY     = "Raspi-Drone OS — Raspberry Pi CM5 base image"
DESCRIPTION = "Minimal Yocto image for the raspi-drone flight controller. \
               Includes SSH access, btop system monitor and tree."
LICENSE     = "MIT"

inherit core-image

# -----------------------------------------------------------------------------
# Image features
# ssh-server-openssh  — installs and enables the OpenSSH daemon
# -----------------------------------------------------------------------------
IMAGE_FEATURES += "ssh-server-openssh"

# -----------------------------------------------------------------------------
# Extra packages
#   btop  — modern, interactive resource monitor (from meta-oe)
#   tree  — directory tree display utility (from meta-oe)
# -----------------------------------------------------------------------------
IMAGE_INSTALL:append = " \
    btop \
    tree \
"
