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

extern crate exonum_build;

use exonum_build::ProtobufGenerator;
use std::env;

fn main() {
    if cfg!(target_os = "macos") && env::var_os("ROCKSDB_STATIC").is_some() {
        // We need to link to libc++.dylib on Mac if using static linkage with RocksDB
        // This is because `librocksdb.a` provided with Homebrew package depends on
        // `libc++.dylib` and in case of static linkage the resulting `libjava_bindigs.dylib`
        // also needs to be linked against `libc++.dylib`.
        // We use dynamic linkage, as there is no static version for this library.
        println!("cargo:rustc-link-lib=dylib=c++");
        // We do the same thing for zstd.a as official rocksdb package depends on this library.
        // We use static linkage to avoid runtime dependency on zstd.
        println!("cargo:rustc-link-lib=static=zstd");
    }

    ProtobufGenerator::with_mod_name("protobuf_mod.rs")
        .with_input_dir("../src/main/proto")
        .add_path("../src/main/proto")
        .add_path("../../messages/src/main/proto/src")
        .generate();
}
