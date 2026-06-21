DESCRIPTION = "Drone OS Image"
LICENSE = "MIT"

inherit core-image

IMAGE_INSTALL += " \
    packagegroup-core-boot \
    kernel-modules \
    btop \
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
    curl \
    tzdata \
    chrony \
    drone-modem \
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
    launch \
    launch-ros \
    launch-xml \
    ros-env \
    drone-foxglove-bridge \
"