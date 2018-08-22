use std::env;
use std::path::PathBuf;

#[cfg(windows)]
pub const PATH_SEPARATOR: &str = ";";
#[cfg(not(windows))]
pub const PATH_SEPARATOR: &str = ":";

/// Joins several classpaths into a single classpath, using the default path separator.
/// Preserves the relative order of class path entries.
pub fn join_paths(parts: &[&str]) -> String {
    parts.join(PATH_SEPARATOR)
}

/// Returns current directory (where executable is placed).
pub fn current_directory() -> PathBuf {
    let mut executable_path =
        env::current_exe().expect("Unable to get current executable location");
    executable_path.pop(); // Drop file name
    executable_path
}
