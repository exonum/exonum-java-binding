/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

use std::fs::File;
use std::io::Read;
use std::path::PathBuf;
use std::sync::Arc;

use java_bindings::jni::{InitArgsBuilder, JNIVersion, JavaVM};
use java_bindings::utils::jni_cache;

/// Kibibyte
pub const KIB: usize = 1024;
/// Mebibyte
pub const MIB: usize = KIB * KIB;

const CONVERSION_FAILED_MESSAGE: &str = "Failed to convert FS path into utf-8";

/// Creates a configured `JavaVM` for benchmarks.
/// _`JavaVM` should be created only *once*._
pub fn create_vm_for_benchmarks() -> Arc<JavaVM> {
    Arc::new(create_vm(false, false))
}

/// Creates a configured `JavaVM` for benchmarks with EJB classes.
/// _`JavaVM` should be created only *once*._
pub fn create_vm_for_benchmarks_with_classes() -> Arc<JavaVM> {
    Arc::new(create_vm(false, true))
}

/// Creates a configured `JavaVM` for tests with EJB classes.
/// _`JavaVM` should be created only *once*._
pub fn create_vm_for_tests_with_classes() -> Arc<JavaVM> {
    Arc::new(create_vm(true, true))
}

/// Creates a configured `JavaVM`.
/// _JavaVM should be created only *once*._
///
/// If `debug` is true, additional checks of correctness of JNI operations are
/// enabled.
///
/// If `with_classes` is true, Java classes, dependencies of the EJB App are
/// included.
fn create_vm(debug: bool, with_classes: bool) -> JavaVM {
    let mut jvm_args_builder = InitArgsBuilder::new()
        .version(JNIVersion::V8)
        .option(&libpath_option());

    if with_classes {
        jvm_args_builder = jvm_args_builder.option(&tests_classpath_option());
        // Enable log4j
        jvm_args_builder = jvm_args_builder.option(&log4j_path_option());
    }
    if debug {
        jvm_args_builder = jvm_args_builder
            // Perform additional checks of correctness of JNI operations
            .option("-Xcheck:jni")
            // Use test-specific JVM options improving performance (see ECR-534)
            .option("-XX:TieredStopAtLevel=1")
            .option("-XX:+UseParallelGC");
    }

    let jvm_args = jvm_args_builder
        .build()
        .unwrap_or_else(|e| panic!("{:#?}", e));

    let vm = JavaVM::new(jvm_args).unwrap_or_else(|e| panic!("{:#?}", e));

    // Initialize JNI cache for testing with fakes
    if with_classes {
        let env = vm.attach_current_thread().unwrap();
        jni_cache::init_cache(&env);
    }

    vm
}

/// Creates a configured `JavaVM` for tests with the limited size of the heap.
pub fn create_vm_for_leak_tests(memory_limit_mib: usize) -> JavaVM {
    let jvm_args = InitArgsBuilder::new()
        .version(JNIVersion::V8)
        .option(&libpath_option())
        .option(&format!("-Xmx{}m", memory_limit_mib))
        .build()
        .unwrap_or_else(|e| panic!("{:#?}", e));

    JavaVM::new(jvm_args).unwrap_or_else(|e| panic!("{:#?}", e))
}

fn tests_classpath_option() -> String {
    format!("-Djava.class.path={}", tests_classpath())
}

pub fn tests_classpath() -> String {
    let classpath_txt_path =
        java_binding_parent_root_dir().join("app/target/ejb-app-classpath.txt");

    let mut classpath = String::new();
    File::open(classpath_txt_path)
        .expect("Can't open classpath.txt")
        .read_to_string(&mut classpath)
        .expect("Failed to read classpath.txt");

    classpath
}

/// Returns a Log4j system property pointing to the configuration file. The file is in
/// the base directory of the `integration_tests` project and must be edited if more detailed
/// Java logs are needed for debugging purposes.
///
/// It requires the log4j-core library to be present on the classpath, which is the case with fakes.
fn log4j_path_option() -> String {
    format!(
        "-Dlog4j.configurationFile={}",
        log4j_path().to_str().unwrap()
    )
}

/// Returns a path to Log4j configuration file to be used in the integration tests.
pub fn log4j_path() -> PathBuf {
    project_root_dir().join("log4j2.xml")
}

fn libpath_option() -> String {
    format!("-Djava.library.path={}", java_library_path())
}

/// Returns path to the java_bindings library for native integration tests.
pub fn java_library_path() -> String {
    let library_path = rust_project_root_dir()
        .join(target_path())
        .canonicalize()
        .expect(
            "Target path not found, but there should be \
             the libjava_bindings dynamically loading library",
        );
    library_path
        .to_str()
        .expect(CONVERSION_FAILED_MESSAGE)
        .to_owned()
}

fn java_binding_parent_root_dir() -> PathBuf {
    rust_project_root_dir()
        .join("../..")
        .canonicalize()
        .unwrap()
}

/// The path to the root directory of the Rust parent module.
fn rust_project_root_dir() -> PathBuf {
    project_root_dir().join("..").canonicalize().unwrap()
}

/// The path to `integration_tests` root directory.
fn project_root_dir() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR"))
}

/// The relative path to a directory that contains runtime dependencies of the integration tests
/// executed with `cargo test`. These dependencies include `java_bindings` library, which
/// is also required and loaded by Java code.
///
/// This path is included in `java.library.path` JVM property, so that `java_bindings` library
/// can be discovered and loaded by Java.
#[cfg(debug_assertions)]
fn target_path() -> &'static str {
    "target/debug/deps"
}

#[cfg(not(debug_assertions))]
fn target_path() -> &'static str {
    "target/release/deps"
}
