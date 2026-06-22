SUMMARY = "ROS 2 resource monitor node for Drone OS"
HOMEPAGE = "https://github.com/raspi-drone/resource-monitor"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit ros_ament_python

SRC_URI = " \
    git://github.com/raspi-drone/resource-monitor.git;protocol=https;branch=main \
    file://resource-monitor.service \
"

# SRCREV = "abc1234..."
SRCREV = "${AUTOREV}"
PV = "0.1+git${SRCPV}"

S = "${WORKDIR}/git/src/resource_monitor"

RDEPENDS:${PN} += " \
    python3 \
    python3-psutil \
    rclpy \
    ros2launch \
    launch \
    launch-ros \
    diagnostic-msgs \
"

do_install:append() {
    ROS_DIR="${D}/opt/ros/jazzy"

    if [ -d "${D}/python3.12" ]; then
        install -d ${ROS_DIR}/lib
        cp -r ${D}/python3.12 ${ROS_DIR}/lib/
        rm -rf ${D}/python3.12
    fi
    if [ -d "${D}/lib/resource_monitor" ]; then
        install -d ${ROS_DIR}/lib
        cp -r ${D}/lib/resource_monitor ${ROS_DIR}/lib/
        rm -rf ${D}/lib
    fi
    if [ -d "${D}/ament_index" ]; then
        install -d ${ROS_DIR}/share
        cp -r ${D}/ament_index ${ROS_DIR}/share/
        rm -rf ${D}/ament_index
    fi
    if [ -d "${D}/resource_monitor" ]; then
        install -d ${ROS_DIR}/share
        cp -r ${D}/resource_monitor ${ROS_DIR}/share/
        rm -rf ${D}/resource_monitor
    fi

    find ${ROS_DIR}/lib/resource_monitor -type f -exec \
        sed -i '1s|^#!.*|#!/usr/bin/env python3|' {} \;

    install -d ${D}${sysconfdir}/udev/rules.d
    echo 'KERNEL=="vcio", MODE="0666"' \
        > ${D}${sysconfdir}/udev/rules.d/99-vcio.rules

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/resource-monitor.service \
        ${D}${systemd_system_unitdir}/resource-monitor.service
}

inherit systemd

SYSTEMD_SERVICE:${PN} = "resource-monitor.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

FILES:${PN} += " \
    ${sysconfdir}/udev/rules.d/99-vcio.rules \
    ${systemd_system_unitdir}/resource-monitor.service \
    /opt/ros/jazzy/lib/python3.12/site-packages/resource_monitor \
    /opt/ros/jazzy/lib/python3.12/site-packages/resource_monitor-*.egg-info \
    /opt/ros/jazzy/lib/resource_monitor \
    /opt/ros/jazzy/share/ament_index/resource_index/packages/resource_monitor \
    /opt/ros/jazzy/share/resource_monitor \
"