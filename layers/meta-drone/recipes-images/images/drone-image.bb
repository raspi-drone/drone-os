DESCRIPTION = "Drone OS Image"
LICENSE = "MIT"

inherit core-image

IMAGE_INSTALL += " \
    packagegroup-core-boot \
    kernel-modules \
    u-boot-fw-utils \
    u-boot-default-env \
"

# network
IMAGE_INSTALL += " \
    networkmanager \
    modemmanager \
    libqmi \
    libmbim \
    wireguard-tools \
    nftables \
    ca-certificates \
    openssl \
    openssh-sshd \
    openssh-sftp-server \
    curl \
    tzdata \
    chrony \
    drone-modem \
    drone-wifi \
    drone-wireguard \
"

# ROS2 Jazzy
IMAGE_INSTALL += " \
    rclcpp \
    rclpy \
    ros2run \
    ros2cli \
    rmw-fastrtps-cpp \
    rmw-cyclonedds-cpp \
    ros2topic \
    ros2node \
    ros2service \
    ros2action \
    ros2param \
    ros2interface \
    ros2launch \
    tf2-geometry-msgs \
    tf2-geometry-msgs-dev \
    tf2-eigen \
    tf2-eigen-dev \
    tf2-sensor-msgs \
    launch \
    launch-ros \
    launch-xml \
    diagnostic-msgs \
    mavros \
    mavros-extras \
    mavros-msgs \
    ros-env \
    drone-dds-config \
    drone-foxglove-bridge \
    drone-mavros \
    drone-resource-monitor \
"

# Mountpoints for external Partition
ROOTFS_POSTPROCESS_COMMAND:append = " create_mountpoints;"

create_mountpoints() {
    mkdir -p ${IMAGE_ROOTFS}/config
    mkdir -p ${IMAGE_ROOTFS}/data
}