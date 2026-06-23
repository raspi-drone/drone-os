from launch import LaunchDescription
from launch.actions import DeclareLaunchArgument
from launch.substitutions import LaunchConfiguration
from launch_ros.actions import Node


def generate_launch_description():
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
            name='mavros',
            parameters=[{
                'fcu_url': LaunchConfiguration('fcu_url'),
                'gcs_url': LaunchConfiguration('gcs_url'),
                'target_system_id': LaunchConfiguration('target_system_id'),
                'target_component_id': LaunchConfiguration('target_component_id'),
            }],
            output='screen',
        ),
    ])