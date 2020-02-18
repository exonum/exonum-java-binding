import os
import unittest

from exonum_launcher.configuration import Configuration
from exonum_launcher.instances import InstanceSpecLoadError

from exonum_instance_configuration_plugin import InstanceSpecLoader

from exonum_instance_configuration_plugin.proto.exonum.java import service_pb2

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
        instance_loader = InstanceSpecLoader()

        instance = config.instances[0]
        serialized_parameters = instance_loader.load_spec(None, instance)
        self.assertEqual(serialized_parameters, b"\n\x07\x74\x65\x73\x74\x69\x6e\x67")

    def test_plugin_custom_message_name(self) -> None:
        config = self.load_config("custom_message_name.yml")
        self.assertEqual(len(config.instances), 1)
        instance_loader = InstanceSpecLoader()

        instance = config.instances[0]
        serialized_parameters = instance_loader.load_spec(None, instance)
        self.assertEqual(serialized_parameters, b"\n\x07\x74\x65\x73\x74\x69\x6e\x67")

    def test_plugin_standard_configuration_message_text(self):
        config = self.load_config("standard_message_text.yml")
        self.assertEqual(len(config.instances), 2)
        instance_loader = InstanceSpecLoader()

        expected_message = service_pb2.ServiceConfiguration()
        expected_message.format = service_pb2.ServiceConfiguration.Format.TEXT
        expected_message.value = "text-configuration\n"

        for instance in config.instances:
            serialized_parameters = instance_loader.load_spec(None, instance)
            self.assertEqual(serialized_parameters, expected_message.SerializeToString())

    def test_plugin_standard_configuration_message_json(self):
        config = self.load_config("standard_message_json.yml")
        self.assertEqual(len(config.instances), 2)
        instance_loader = InstanceSpecLoader()

        expected_message = service_pb2.ServiceConfiguration()
        expected_message.format = service_pb2.ServiceConfiguration.Format.JSON
        expected_message.value = "{\"some\": [\"json\", {}]}\n"

        for instance in config.instances:
            serialized_parameters = instance_loader.load_spec(None, instance)
            self.assertEqual(serialized_parameters, expected_message.SerializeToString())

    def test_plugin_standard_configuration_message_properties(self):
        config = self.load_config("standard_message_properties.yml")
        self.assertEqual(len(config.instances), 2)
        instance_loader = InstanceSpecLoader()

        expected_message = service_pb2.ServiceConfiguration()
        expected_message.format = service_pb2.ServiceConfiguration.Format.PROPERTIES
        expected_message.value = "Truth=Beauty"

        for instance in config.instances:
            serialized_parameters = instance_loader.load_spec(None, instance)
            self.assertEqual(serialized_parameters, expected_message.SerializeToString())

    def test_plugin_errors_invalid_config(self) -> None:
        config = self.load_config("invalid_config.yml")
        self.assertEqual(len(config.instances), 8)
        instance_loader = InstanceSpecLoader()

        for instance in config.instances:
            with self.assertRaisesRegex(InstanceSpecLoadError, instance.name):
                instance_loader.load_spec(None, instance)


if __name__ == '__main__':
    unittest.main()
