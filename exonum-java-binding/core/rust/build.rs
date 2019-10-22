extern crate exonum_build;

use exonum_build::protobuf_generate;
use std::env::var_os;

fn main() {
    // We need to link to libc++.dylib on Mac if using static linkage with RocksDB
    // This is because `librocksdb.a` provided with Homebrew package depends on
    // `libc++.dylib` and in case of static linkage the resulting `libjava_bindigs.dylib`
    // also needs to be linked against `libc++.dylib`.
    if cfg!(target_os = "macos") && var_os("ROCKSDB_STATIC").is_some() {
        println!("cargo:rustc-link-lib=dylib=c++");
    }

    protobuf_generate("../src/main/proto", &["src/proto"], "protobuf_mod.rs");
}
