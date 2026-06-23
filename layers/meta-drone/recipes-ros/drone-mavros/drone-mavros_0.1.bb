SUMMARY = "MAVROS launch configuration for Drone OS"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit allarch

SRC_URI = " \
    file://mavros.launch.py \
    file://mavros.service \
"

RDEPENDS:${PN} = " \
    mavros \
    mavros-extras \
    ros2launch \
    launch \
    launch-ros \
    geographiclib-datasets \
"

do_install() {
    install -d ${D}/opt/ros/jazzy/share/drone_mavros/launch
    install -m 0644 ${WORKDIR}/mavros.launch.py \
        ${D}/opt/ros/jazzy/share/drone_mavros/launch/mavros.launch.py

    install -d ${D}/opt/ros/jazzy/share/ament_index/resource_index/packages
    touch ${D}/opt/ros/jazzy/share/ament_index/resource_index/packages/drone_mavros

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/mavros.service \
        ${D}${systemd_system_unitdir}/mavros.service
}

inherit systemd

SYSTEMD_SERVICE:${PN} = "mavros.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

FILES:${PN} = " \
    /opt/ros/jazzy/share/drone_mavros \
    /opt/ros/jazzy/share/ament_index/resource_index/packages/drone_mavros \
    ${systemd_system_unitdir}/mavros.service \
"