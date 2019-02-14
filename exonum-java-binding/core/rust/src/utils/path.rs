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
pub fn executable_directory() -> PathBuf {
    let mut executable_path =
        env::current_exe().expect("Unable to get current executable location");
    executable_path.pop(); // Drop file name.
    executable_path
}

#[cfg(test)]
mod tests {
    use super::*;
    const FOO: &str = "foo";
    const BAR: &str = "bar";
    const BAZ: &str = "baz";

    #[cfg(windows)]
    const FOO_BAR_BAZ: &str = "foo;bar;baz";
    #[cfg(not(windows))]
    const FOO_BAR_BAZ: &str = "foo:bar:baz";

    #[test]
    fn join_paths_preserves_order() {
        let result = join_paths(&[FOO, BAR, BAZ]);
        assert_eq!(result, FOO_BAR_BAZ);
    }
}
