import os
import subprocess
from shutil import copy
from pathlib import Path
from tests_profile import TestsProfile


def clear_cargo(manifest_dir):
    manifest_path = os.path.join(manifest_dir, "Cargo.toml")
    subprocess.run(["cargo", "clean", "--manifest-path", manifest_path], check=True)


if __name__ == '__main__':
    tests_profile = TestsProfile()

    ejb_rust_dir = os.path.join(tests_profile.project_root, "core", "rust")
    clear_cargo(ejb_rust_dir)

    packaging_base_dir = os.path.join(ejb_rust_dir, "target", "debug")
    packaging_etc_dir = os.path.join(packaging_base_dir, "etc")
    packaging_native_lib_dir = os.path.join(packaging_base_dir, "lib", "native")

    Path(packaging_base_dir).mkdir(parents=True, exist_ok=True)
    Path(packaging_etc_dir).mkdir(parents=True, exist_ok=True)
    Path(packaging_native_lib_dir).mkdir(parents=True, exist_ok=True)

    copy(os.path.join(tests_profile.std_lib_dir, tests_profile.std_lib_name), packaging_native_lib_dir)

    license_path = Path(tests_profile.project_root).parent.joinpath("LICENSE")
    copy(license_path, packaging_etc_dir)
    license_path = Path(tests_profile.project_root).joinpath("LICENSES-THIRD-PARTY.TXT")
    copy(license_path, packaging_etc_dir)

    copy(os.path.join(ejb_rust_dir, "exonum-java", "log4j-fallback.xml"), packaging_etc_dir)
    copy(os.path.join(ejb_rust_dir, "exonum-java", "README.md"), packaging_etc_dir)

    rust_lib_path = os.path.join(packaging_base_dir, "deps", tests_profile.java_binding_lib_name)
    exonum_java_name = tests_profile.exonum_java_name
    subprocess.run([f"mvn package --activate-profiles package-app "
                    f"-pl :exonum-java-binding-packaging -am "
                    f"-DskipTests -Dbuild.mode=debug "
                    f"-DskipRustLibBuild "
                    f"-Drust.libraryPath={rust_lib_path} "
                    f"-Drust.compiler.version=stable "
                    f"-Dpackaging.exonumJavaName={exonum_java_name}"],
                   shell=True, check=True)
