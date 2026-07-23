from launch import LaunchDescription
from launch.actions import DeclareLaunchArgument
from launch.substitutions import LaunchConfiguration, PathJoinSubstitution
from launch_ros.actions import Node
from launch_ros.substitutions import FindPackageShare


def generate_launch_description():
    mavros_share = FindPackageShare('mavros')
    return LaunchDescription([
        DeclareLaunchArgument('fcu_url', default_value='serial:///dev/ttyAMA2:921600',
                              description='FCU connection URL'),
        DeclareLaunchArgument('gcs_url', default_value='udp-b://0.0.0.0:14550@14555',
                              description='GCS connection URL'),
        DeclareLaunchArgument('target_system_id', default_value='1'),
        DeclareLaunchArgument('target_component_id', default_value='1'),
        Node(
            package='mavros',
            executable='mavros_node',
            # No explicit `name=` here: mavros_node.cpp constructs one
            # rclcpp::Node per plugin, each with its own unique name (e.g.
            # "imu", "esc_status"). Those sub-nodes inherit this process's
            # global ROS args, so an explicit name here becomes a global
            # `-r __node:=<name>` remap that overwrites every plugin's node
            # name too - collapsing all of them onto the same name and
            # making their "~/..." topics collide under one shared prefix
            # (e.g. /mavros/mavros/attitude instead of
            # /mavros/setpoint_attitude/attitude and /mavros/imu/data).
            parameters=[
                # upstream-maintained ArduPilot plugin denylist + frame_id/TF
                # defaults (this drone runs ArduCopter, not PX4).
                PathJoinSubstitution([mavros_share, 'launch', 'apm_pluginlists.yaml']),
                PathJoinSubstitution([mavros_share, 'launch', 'apm_config.yaml']),
                {
                    'fcu_url': LaunchConfiguration('fcu_url'),
                    'gcs_url': LaunchConfiguration('gcs_url'),
                    'target_system_id': LaunchConfiguration('target_system_id'),
                    'target_component_id': LaunchConfiguration('target_component_id'),
                },
            ],
            output='screen',
            # mavros_node has been observed to crash (SIGSEGV) right after
            # an FCU reconnects. `ros2 launch` treats a child process death
            # as a shutdown trigger and exits 0 itself, so systemd's
            # Restart=on-failure never sees a failure. respawn restarts just
            # this process without tearing down the launch session.
            respawn=True,
            respawn_delay=2.0,
        ),
    ])