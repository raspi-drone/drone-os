SUMMARY = "ROS 2 Jazzy runtime environment"
LICENSE = "MIT"

inherit allarch

do_install() {
    install -d ${D}${sysconfdir}/profile.d

    cat > ${D}${sysconfdir}/profile.d/ros2.sh << 'EOF'
export ROS_DISTRO=jazzy
export PATH=/opt/ros/jazzy/bin:$PATH
export PYTHONPATH=/opt/ros/jazzy/lib/python3.12/site-packages:$PYTHONPATH
export LD_LIBRARY_PATH=/opt/ros/jazzy/lib:$LD_LIBRARY_PATH
export AMENT_PREFIX_PATH=/opt/ros/jazzy
export COLCON_PREFIX_PATH=/opt/ros/jazzy
EOF
}

FILES:${PN} += "${sysconfdir}/profile.d/ros2.sh"