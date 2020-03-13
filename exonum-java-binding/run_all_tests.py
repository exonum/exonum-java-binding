import os
import subprocess
from tests_profile import TestsProfile


def run_maven_tests():
    subprocess.run(["mvn install -Drust.compiler.version=stable --activate-profiles ci-build"], shell=True, check=True)


def run_native_integration_tests(project_root):
    manifest_path = os.path.join(project_root, "core", "rust", "integration_tests", "Cargo.toml")
    subprocess.run(["cargo", "test", "--manifest-path", manifest_path], shell=True, check=True)


if __name__ == '__main__':
    tests_profile = TestsProfile()
    run_maven_tests()
    run_native_integration_tests(tests_profile.project_root)
