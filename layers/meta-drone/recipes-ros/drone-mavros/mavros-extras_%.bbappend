# tf2_eigen (jazzy) never defined tf2::fromMsg(geometry_msgs::msg::Transform,
# Eigen::Affine3d&) - only transformToEigen(Transform) -> Eigen::Isometry3d.
# fake_gps.cpp calls the missing overload, which silently resolves to tf2's
# unspecialized generic template at compile time and only fails at dlopen()
# with "undefined symbol: ...tf2::fromMsg<...Transform_...Eigen::Transform...>".
# Use transformToEigen() instead, matching what tf2_eigen actually provides.
do_configure:prepend() {
    sed -i \
        -e 's/tf2::fromMsg(trans->transform, pos_enu);/pos_enu = tf2::transformToEigen(trans->transform);/' \
        -e 's/tf2::fromMsg(trans.transform, pos_enu);/pos_enu = tf2::transformToEigen(trans.transform);/' \
        ${S}/src/plugins/fake_gps.cpp
}
