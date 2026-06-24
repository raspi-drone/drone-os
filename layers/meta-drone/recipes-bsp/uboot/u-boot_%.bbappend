COMPATIBLE_MACHINE:raspberrypi5 = "raspberrypi5"

# RPi5 U-Boot defconfig
UBOOT_MACHINE:raspberrypi5 = "rpi_arm64_defconfig"

# fw_env für RAUC (later Phase, just prep...)
UBOOT_ENV_SIZE:raspberrypi5 = "0x4000"
UBOOT_ENV_OFFSET:raspberrypi5 = "0x400000"