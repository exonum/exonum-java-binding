import os
import subprocess
import sys
import fnmatch
from shutil import copy
from pathlib import Path


def shared_lib_extension():
    if sys.platform == 'linux2':
        return ".so"
    elif sys.platform == 'darwin':
        return ".dylib"
    elif sys.platform == 'win32':
        return ".dll"
    else:
        raise Exception(f"This OS ({sys.platform}) is unsupported")


def jvm_lib_name():
    if sys.platform == 'win32':
        return "jvm" + shared_lib_extension()
    else:
        return "libjvm" + shared_lib_extension()


def get_java_lib_dir(java_home):
    for root, _, filenames in os.walk(java_home):
        if jvm_lib_name() in filenames:
            return root
    raise Exception(f"Invalid JAVA_HOME, jvm library not found: {java_home}")


def get_std_lib_location(rust_sysroot):
    lib_name = "*std-*" + shared_lib_extension()
    for root, _, filenames in os.walk(rust_sysroot):
        for filename in fnmatch.filter(filenames, lib_name):
            return root, filename


def get_java_home():
    if not os.environ["JAVA_HOME"] is None:
        return os.environ["JAVA_HOME"]
    result = subprocess.run(["java", "-XshowSettings:properties", "-version"], capture_output=True, check=True)
    for line in result.stderr.decode("utf-8").split(os.linesep):
        if "java.home" in line:
            java_home = line.split("=")[1].strip()
            return java_home


def get_rust_sysroot():
    result = subprocess.run(["rustup", "run", "stable", "rustc", "--print", "sysroot"], capture_output=True, check=True)
    rust_sysroot = result.stdout.decode("utf-8").strip()
    return rust_sysroot


def get_project_root():
    return os.path.dirname(os.path.realpath(__file__))


def create_path(java_home, std_lib_dir, project_root):
    return os.environ["PATH"] + os.pathsep + get_java_lib_dir(java_home) + os.pathsep + \
           std_lib_dir + os.pathsep + os.path.join(project_root, "core", "rust", "target", "debug")


def set_rustflags(std_lib_dir, java_lib_dir):
    if sys.platform == 'win32':
        new_rustflags = " "
    else:
        new_rustflags = f"-C link-arg=-Wl,-rpath,{std_lib_dir} -C link-arg=-Wl,-rpath,{java_lib_dir}"

    if ("RUSTFLAGS" in os.environ) and (not os.environ["RUSTFLAGS"] == new_rustflags):
        print("[WARNING]: RUSTFLAGS variable is set and will be overridden. \
        If you need to pass extra compiler flags, edit 'run_all_tests.py' script")
        print(f"Set RUSTFLAGS={os.environ['RUSTFLAGS']}")
        print(f"New RUSTFLAGS={new_rustflags}")
    os.environ["RUSTFLAGS"] = new_rustflags


def tests_profile():
    java_home = get_java_home()
    os.environ["JAVA_HOME"] = java_home
    print(f"JAVA_HOME={java_home}")

    java_lib_dir = get_java_lib_dir(java_home)

    rust_sysroot = get_rust_sysroot()

    std_lib_dir, std_lib_name = get_std_lib_location(rust_sysroot)
    print(f"RUST_LIB_DIR={std_lib_dir}")

    set_rustflags(std_lib_dir, java_lib_dir)

    project_root = get_project_root()

    path = create_path(java_home, std_lib_dir, project_root)
    os.environ["PATH"] = path
    sys.stdout.flush()


def clear_cargo(ejb_rust_dir):
    manifest_path = os.path.join(ejb_rust_dir, "Cargo.toml")
    subprocess.run(["cargo", "clean", "--manifest-path", manifest_path], check=True)


def java_bindings_lib():
    if sys.platform == 'win32':
        return "java_bindings.dll"
    elif sys.platform == 'darwin':
        return "libjava_bindings.dylib"
    elif sys.platform == 'linux2':
        return "libjava_bindings.so"
    else:
        raise Exception(f"This OS ({sys.platform}) is unsupported")


def exonum_java_name():
    if sys.platform == 'win32':
        return "exonum-java.exe"
    else:
        return "exonum-java"


if __name__ == '__main__':
    tests_profile()

    ejb_rust_dir = os.path.join(get_project_root(), "core", "rust")
    clear_cargo(ejb_rust_dir)

    packaging_base_dir = os.path.join(ejb_rust_dir, "target", "debug")
    packaging_etc_dir = os.path.join(packaging_base_dir, "etc")
    packaging_native_lib_dir = os.path.join(packaging_base_dir, "lib", "native")

    Path(packaging_base_dir).mkdir(parents=True, exist_ok=True)
    Path(packaging_etc_dir).mkdir(parents=True, exist_ok=True)
    Path(packaging_native_lib_dir).mkdir(parents=True, exist_ok=True)

    std_lib_dir, std_lib_name = get_std_lib_location(get_rust_sysroot())
    copy(os.path.join(std_lib_dir, std_lib_name), packaging_native_lib_dir)

    license_path = Path(get_project_root()).parent.joinpath("LICENSE")
    copy(license_path, packaging_etc_dir)
    license_path = Path(get_project_root()).joinpath("LICENSES-THIRD-PARTY.TXT")
    copy(license_path, packaging_etc_dir)

    copy(os.path.join(ejb_rust_dir, "exonum-java", "log4j-fallback.xml"), packaging_etc_dir)
    copy(os.path.join(ejb_rust_dir, "exonum-java", "README.md"), packaging_etc_dir)

    rust_lib_path = os.path.join(packaging_base_dir, "deps", java_bindings_lib())
    exonum_java_name = exonum_java_name()
    subprocess.run([f"mvn package --activate-profiles package-app -pl :exonum-java-binding-packaging -am -DskipTests -Dbuild.mode=debug -DskipRustLibBuild -Drust.libraryPath={rust_lib_path} -Drust.compiler.version=stable -Dpackaging.exonumJavaName={exonum_java_name}"], shell=True, check=True)
