DESCRIPTION = "Drone OS Image"
LICENSE = "MIT"

inherit core-image

IMAGE_INSTALL += " \
    packagegroup-core-boot \
    kernel-modules \
    networkmanager \
    modemmanager \
    libqmi \
    libmbim \
    wireguard-tools \
    drone-network \
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