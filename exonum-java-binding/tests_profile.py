import sys
import os
import fnmatch
import subprocess


class TestsProfile:
    def __init__(self):
        self.shared_lib_extension = self._shared_lib_extension()
        self.jvm_lib_name = self._jvm_lib_name()
        self.java_home = self._get_java_home()
        self.java_lib_dir = self._get_java_lib_dir()
        self.rust_sysroot = self._get_rust_sysroot()
        self.std_lib_dir, self.std_lib_name = self._get_std_lib_location()
        self.project_root = self._get_project_root()

        self.java_binding_lib_name = self._java_bindings_lib_name()
        self.exonum_java_name = self._exonum_java_name()

        self._set_environment_variables()

    def _set_environment_variables(self):
        os.environ["JAVA_HOME"] = self.java_home
        print(f"JAVA_HOME={self.java_home}")
        self._set_rustflags()
        os.environ["PATH"] = self._create_path()

    def _get_java_lib_dir(self):
        for root, _, filenames in os.walk(self.java_home):
            if self._jvm_lib_name() in filenames:
                return root
        raise Exception(f"Invalid JAVA_HOME, jvm library not found: {self.java_home}")

    def _get_std_lib_location(self):
        lib_name = "*std-*" + self._shared_lib_extension()
        for root, _, filenames in os.walk(self.rust_sysroot):
            for filename in fnmatch.filter(filenames, lib_name):
                return root, filename

        raise Exception(f"Rust std lib not found in {self.rust_sysroot}")

    @staticmethod
    def _get_java_home():
        if not os.environ["JAVA_HOME"] is None:
            return os.environ["JAVA_HOME"]
        result = subprocess.run(["java", "-XshowSettings:properties", "-version"], capture_output=True, check=True)
        for line in result.stderr.decode("utf-8").split(os.linesep):
            if "java.home" in line:
                java_home = line.split("=")[1].strip()
                return java_home

    @staticmethod
    def _get_rust_sysroot():
        result = subprocess.run(["rustup", "run", "stable", "rustc", "--print", "sysroot"], capture_output=True, check=True)
        rust_sysroot = result.stdout.decode("utf-8").strip()
        return rust_sysroot

    @staticmethod
    def _get_project_root():
        return os.path.dirname(os.path.realpath(__file__))

    def _create_path(self):
        return os.environ["PATH"] + os.pathsep + \
               self.java_lib_dir + os.pathsep + \
               self.std_lib_dir + os.pathsep + \
               os.path.join(self.project_root, "core", "rust", "target", "debug")

    def _set_rustflags(self):
        if sys.platform == 'win32':
            new_rustflags = " "
        else:
            new_rustflags = f"-C link-arg=-Wl,-rpath,{self.std_lib_dir} -C link-arg=-Wl,-rpath,{self.java_lib_dir}"

        if ("RUSTFLAGS" in os.environ) and (not os.environ["RUSTFLAGS"] == new_rustflags):
            print("[WARNING]: RUSTFLAGS variable is set and will be overridden. \
            If you need to pass extra compiler flags, edit 'run_all_tests.py' script")
            print(f"Set RUSTFLAGS={os.environ['RUSTFLAGS']}")
            print(f"New RUSTFLAGS={new_rustflags}")
        os.environ["RUSTFLAGS"] = new_rustflags

    @staticmethod
    def _java_bindings_lib_name():
        if sys.platform == 'win32':
            return "java_bindings" + TestsProfile._shared_lib_extension()
        else:
            return "libjava_bindings" + TestsProfile._shared_lib_extension()

    @staticmethod
    def _exonum_java_name():
        if sys.platform == 'win32':
            return "exonum-java.exe"
        else:
            return "exonum-java"

    @staticmethod
    def _shared_lib_extension():
        if sys.platform == 'linux2':
            return ".so"
        elif sys.platform == 'darwin':
            return ".dylib"
        elif sys.platform == 'win32':
            return ".dll"
        else:
            raise Exception(f"This OS ({sys.platform}) is unsupported")

    @staticmethod
    def _jvm_lib_name():
        if sys.platform == 'win32':
            return "jvm" + TestsProfile._shared_lib_extension()
        else:
            return "libjvm" + TestsProfile._shared_lib_extension()
