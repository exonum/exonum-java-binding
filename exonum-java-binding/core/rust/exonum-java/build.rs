// Copyright 2020 The Exonum Team
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use std::env;

fn main() {
    let lib_paths = [
        "ROCKSDB_LIB_DIR",
        "SNAPPY_LIB_DIR",
        "LZ4_LIB_DIR",
        "BZ2_LIB_DIR",
        "Z_LIB_DIR",
    ];

    lib_paths
        .iter()
        .filter_map(|lib| env::var(lib).ok())
        .for_each(|path| println!("cargo:rustc-search-lib=native={}", path));

    let libs_linking_types = [
        "ROCKSDB_STATIC",
        "SNAPPY_STATIC",
        "LZ4_STATIC",
        "BZ2_STATIC",
        "Z_STATIC",
    ];

    for lib_linking_type in &libs_linking_types {
        let lib_parts: Vec<&str> = lib_linking_type.split('_').collect();
        let lib_name = lib_parts[0].to_lowercase();
        let linking_type = if env::var_os(lib_linking_type).is_some() {
            "static"
        } else {
            "dylib"
        };
        println!("cargo:rustc-link-lib={}={}", linking_type, lib_name);
    }

    if cfg!(target_os = "macos") && env::var_os("ROCKSDB_STATIC").is_some() {
        // We need to link to libc++.dylib on Mac if using static linkage with RocksDB
        // This is because `librocksdb.a` provided with Homebrew package depends on
        // `libc++.dylib` and in case of static linkage the resulting `libjava_bindigs.dylib`
        // also needs to be linked against `libc++.dylib`.
        // We use dynamic linkage, as there is no static version for this library.
        println!("cargo:rustc-link-lib=dylib=c++");
        // We do the same thing for zstd.a as official rocksdb package depends on this library.
        // We use static linkage to avoid runtime dependency on zstd.
        let zstd_lib_path =
            env::var("ZSTD_LIB_DIR").unwrap_or_else(|_| "/usr/local/lib".to_owned());
        println!("cargo:rustc-link-search=native={}", zstd_lib_path);
        println!("cargo:rustc-link-lib=static=zstd");
    }
}
