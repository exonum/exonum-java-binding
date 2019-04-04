use std::env;
use std::path::PathBuf;

/// Returns current directory (where executable is placed).
pub fn executable_directory() -> PathBuf {
    let mut executable_path =
        env::current_exe().expect("Unable to get current executable location");
    executable_path.pop(); // Drop file name.
    executable_path
}
