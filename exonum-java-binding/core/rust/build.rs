use std::env::var_os;

fn main() {
    if cfg!(target_os = "macos") && var_os("ROCKSDB_STATIC").is_some() {
        println!("cargo:rustc-link-lib=dylib=c++");
    }
}
