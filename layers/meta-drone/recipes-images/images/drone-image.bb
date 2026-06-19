DESCRIPTION = "Drone OS Image"
LICENSE = "MIT"

inherit core-image

IMAGE_INSTALL += " \
    packagegroup-core-boot \
    kernel-modules \
"

# network
IMAGE_INSTALL += " \
    networkmanager \
    modemmanager \
    libqmi \
    libmbim \
    wireguard-tools \
    drone-network \
    ca-certificates \
    openssl \
    curl \
    tzdata \
    chrony \
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
    ros-env \
"