use std::{env, fs};

use utils::executable_directory;

/// Returns path to <ejb-app location>/lib directory in an absolute form.
pub fn absolute_library_path() -> String {
    let library_path = {
        let mut executable_directory = executable_directory();
        executable_directory.push("lib/native");
        executable_directory
    };
    library_path.to_string_lossy().into_owned()
}

pub fn system_classpath() -> String {
    let mut jars = Vec::new();
    let jars_directory = {
        let mut executable_directory = executable_directory();
        executable_directory.push("lib/java");
        executable_directory
    };
    for entry in fs::read_dir(jars_directory).expect("Could not read java classes directory") {
        let file = entry.unwrap();
        if file.file_type().unwrap().is_file() {
            jars.push(file.path());
        } else {
            continue;
        }
    }

    let jars = jars.iter().map(|p| p.to_str().unwrap());
    env::join_paths(jars).unwrap().into_string().unwrap()
}

/// Panics if `_JAVA_OPTIONS` environmental variable is set.
pub fn panic_if_java_options() {
    if env::var("_JAVA_OPTIONS").is_ok() {
        panic!(
            "_JAVA_OPTIONS environment variable is set. \
             Due to the fact that it will overwrite any JVM settings, \
             including ones set by EJB internally, this variable is \
             forbidden for EJB applications.\n\
             It is recommended to use `--jvm-args-append` and `--jvm-args-prepend` command-line \
             parameters for setting custom JVM parameters."
        );
    }
}
