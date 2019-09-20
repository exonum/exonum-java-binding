extern crate exonum_build;

use std::env::var_os;
use exonum_build::{/* get_exonum_protobuf_files_path,*/ protobuf_generate};

fn main() {
    // We need to link to libc++.dylib on Mac if using static linkage with RocksDB
    // This is because `librocksdb.a` provided with Homebrew package depends on
    // `libc++.dylib` and in case of static linkage the resulting `libjava_bindigs.dylib`
    // also needs to be linked against `libc++.dylib`.
    if cfg!(target_os = "macos") && var_os("ROCKSDB_STATIC").is_some() {
        println!("cargo:rustc-link-lib=dylib=c++");
    }

    //let exonum_protos = get_exonum_protobuf_files_path();
    protobuf_generate(
        "../src/main/proto",
        &["src/proto", /*&exonum_protos */],
        "protobuf_mod.rs",
    );

}
