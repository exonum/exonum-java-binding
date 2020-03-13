import os
import sys
import subprocess
from shutil import copy
from pathlib import Path
from tests_profile import TestsProfile


def set_packaging_rustflags():
    # No need on Windows
    if sys.platform == 'win32':
        return

    if sys.platform == 'linux2':
        prefix = "\$ORIGIN"
    elif sys.platform == 'darwin':
        prefix = "@loader_path"
    else:
        raise Exception(f"This OS ({sys.platform}) is not supported")

    lib_path_relative_to_exe = prefix + "/lib/native"
    rustflags = f"-C link-arg=-Wl,-rpath,{lib_path_relative_to_exe} -C link-arg=-Wl,-rpath,${prefix}"
    print(f"Setting new RUSTFLAGS={rustflags}")
    os.environ["RUSTFLAGS"] = rustflags


def clear_cargo(manifest_dir):
    manifest_path = os.path.join(manifest_dir, "Cargo.toml")
    subprocess.run(["cargo", "clean", "--manifest-path", manifest_path], check=True)


if __name__ == '__main__':
    tests_profile = TestsProfile()

    # Clear Rust artifacts
    ejb_rust_dir = os.path.join(tests_profile.project_root, "core", "rust")
    clear_cargo(ejb_rust_dir)

    # Prepare directories
    packaging_base_dir = os.path.join(ejb_rust_dir, "target", "debug")
    packaging_etc_dir = os.path.join(packaging_base_dir, "etc")
    packaging_native_lib_dir = os.path.join(packaging_base_dir, "lib", "native")

    Path(packaging_base_dir).mkdir(parents=True, exist_ok=True)
    Path(packaging_etc_dir).mkdir(parents=True, exist_ok=True)
    Path(packaging_native_lib_dir).mkdir(parents=True, exist_ok=True)

    # Copy Rust's std lib
    copy(os.path.join(tests_profile.std_lib_dir, tests_profile.std_lib_name), packaging_native_lib_dir)

    # Copy licenses
    license_path = Path(tests_profile.project_root).parent.joinpath("LICENSE")
    copy(license_path, packaging_etc_dir)
    license_path = Path(tests_profile.project_root).joinpath("LICENSES-THIRD-PARTY.TXT")
    copy(license_path, packaging_etc_dir)

    # Copy logger fallback configuration and README.md
    copy(os.path.join(ejb_rust_dir, "exonum-java", "log4j-fallback.xml"), packaging_etc_dir)
    copy(os.path.join(ejb_rust_dir, "exonum-java", "README.md"), packaging_etc_dir)

    # Set RUSTFLAGS
    set_packaging_rustflags()

    # Package!
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
