import os
import subprocess
import sys
import fnmatch


def shared_lib_extension():
    if os.name == 'posix':
        return ".so"
    elif os.name == 'os2':
        return ".dylib"
    elif os.name == 'nt':
        return ".dll"
    else:
        raise Exception(f"This OS ({os.name}) is unsupported")


def jvm_lib_name():
    return "jvm" + shared_lib_extension()


def get_java_lib_dir(java_home):
    for root, _, filenames in os.walk(java_home):
        if jvm_lib_name() in filenames:
            return root
    raise Exception(f"Invalid JAVA_HOME, jvm library not found: {java_home}")


def get_std_lib_location(rust_sysroot):
    lib_name = "std-*" + shared_lib_extension()
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
    if os.name == 'nt':
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


def run_maven_tests():
    subprocess.run(["mvn", "install", "-Drust.compiler.version=stable", "--activate-profiles", "ci-build"], shell=True,
                   check=True)


def run_native_integration_tests(project_root):
    manifest_path = os.path.join(project_root, "core", "rust", "integration_tests", "Cargo.toml")
    subprocess.run(["cargo", "test", "--manifest-path", manifest_path], shell=True, check=True)


if __name__ == '__main__':
    tests_profile()
    run_maven_tests()
    run_native_integration_tests(get_project_root())
