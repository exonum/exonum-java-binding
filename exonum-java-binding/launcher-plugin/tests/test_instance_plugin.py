import os
import unittest

from exonum_launcher.configuration import Configuration
from exonum_launcher.instances import InstanceSpecLoadError

from exonum_java_instance_plugin import JavaInstanceSpecLoader

_DIR_PATH = os.path.dirname(os.path.realpath(__file__))


class TestInstancePlugin(unittest.TestCase):
    @staticmethod
    def load_config(file_name: str) -> Configuration:
        """Loads Configuration from the sample .yml file"""
        config_path = os.path.join(_DIR_PATH, "test_data", file_name)
        return Configuration.from_yaml(config_path)

    def test_plugin_encodes_config_correctly(self) -> None:
        config = self.load_config("instance_plugin.yml")
        self.assertEqual(len(config.instances), 1)
        instance_loader = JavaInstanceSpecLoader()

        for instance in config.instances:
            spec = instance_loader.load_spec(None, instance)
            # https://developers.google.com/protocol-buffers/docs/encoding#strings
            self.assertEqual(spec, b"\x12\x07\x74\x65\x73\x74\x69\x6e\x67")

    def test_plugin_errors_no_config_field(self) -> None:
        config = self.load_config("no_config.yml")
        self.assertEqual(len(config.instances), 4)
        instance_loader = JavaInstanceSpecLoader()

        for instance in config.instances:
            with self.assertRaisesRegex(InstanceSpecLoadError, instance.name):
                instance_loader.load_spec(None, instance)
