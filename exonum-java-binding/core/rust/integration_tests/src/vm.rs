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

use java_bindings::jni::{InitArgsBuilder, JNIVersion, JavaVM};
use java_bindings::utils::jni_cache;

use std::fs::File;
use std::io::Read;
use std::path::{Path, PathBuf};
use std::sync::Arc;

/// Kibibyte
pub const KIB: usize = 1024;
/// Mebibyte
pub const MIB: usize = KIB * KIB;

const CONVERSION_FAILED_MESSAGE: &str = "Failed to convert FS path into utf-8";

/// Creates a configured `JavaVM` for benchmarks.
/// _`JavaVM` should be created only *once*._
#[allow(dead_code)]
pub fn create_vm_for_benchmarks() -> Arc<JavaVM> {
    Arc::new(create_vm(false, false))
}

/// Creates a configured `JavaVM` for benchmarks with fake classes.
/// _`JavaVM` should be created only *once*._
#[allow(dead_code)]
pub fn create_vm_for_benchmarks_with_fakes() -> Arc<JavaVM> {
    Arc::new(create_vm(false, true))
}

/// Creates a configured `JavaVM` for tests.
/// _`JavaVM` should be created only *once*._
#[allow(dead_code)]
pub fn create_vm_for_tests() -> Arc<JavaVM> {
    Arc::new(create_vm(true, false))
}

/// Creates a configured `JavaVM` for tests with fake classes.
/// _`JavaVM` should be created only *once*._
#[allow(dead_code)]
pub fn create_vm_for_tests_with_fake_classes() -> Arc<JavaVM> {
    Arc::new(create_vm(true, true))
}

/// Creates a configured `JavaVM`.
/// _JavaVM should be created only *once*._
fn create_vm(debug: bool, with_fakes: bool) -> JavaVM {
    let mut jvm_args_builder = InitArgsBuilder::new()
        .version(JNIVersion::V8)
        .option(&get_libpath_option());

    if with_fakes {
        jvm_args_builder = jvm_args_builder.option(&get_fakes_classpath_option());
        // Enable log4j
        jvm_args_builder = jvm_args_builder.option(&get_log4j_path_option());
    }
    if debug {
        jvm_args_builder = jvm_args_builder.option("-Xcheck:jni");
    }

    let jvm_args = jvm_args_builder
        .build()
        .unwrap_or_else(|e| panic!("{:#?}", e));

    let vm = JavaVM::new(jvm_args).unwrap_or_else(|e| panic!("{:#?}", e));

    // Initialize JNI cache for testing with fakes
    if with_fakes {
        let env = vm.attach_current_thread().unwrap();
        jni_cache::init_cache(&env);
    }

    vm
}

/// Creates a configured `JavaVM` for tests with the limited size of the heap.
pub fn create_vm_for_leak_tests(memory_limit_mib: usize) -> JavaVM {
    let jvm_args = InitArgsBuilder::new()
        .version(JNIVersion::V8)
        .option(&get_libpath_option())
        .option(&format!("-Xmx{}m", memory_limit_mib))
        .build()
        .unwrap_or_else(|e| panic!("{:#?}", e));

    JavaVM::new(jvm_args).unwrap_or_else(|e| panic!("{:#?}", e))
}

fn get_fakes_classpath_option() -> String {
    format!("-Djava.class.path={}", get_fakes_classpath())
}

pub fn get_fakes_classpath() -> String {
    let classpath_txt_path = java_binding_parent_root_dir().join("fakes/target/ejb-fakes-classpath.txt");

    let mut class_path = String::new();
    File::open(classpath_txt_path)
        .expect("Can't open classpath.txt")
        .read_to_string(&mut class_path)
        .expect("Failed to read classpath.txt");

    let fakes_path = java_binding_parent_root_dir().join("fakes/target/classes/");
    let fakes_classes = fakes_path.to_str().expect(CONVERSION_FAILED_MESSAGE);

    // should be used `;` as path separator on Windows [https://jira.bf.local/browse/ECR-587]
    format!("{}:{}", class_path, fakes_classes)
}

/// Returns a Log4j system property pointing to the configuration file. The file is in
/// the base directory of the `integration_tests` project and must be edited if more detailed
/// Java logs are needed for debugging purposes.
///
/// It requires the log4j-core library to be present on the classpath, which is the case with fakes.
fn get_log4j_path_option() -> String {
    format!("-Dlog4j.configurationFile={}", get_log4j_path())
}

/// Returns a path to Log4j configuration file to be used in the integration tests.
pub fn get_log4j_path() -> String {
    project_root_dir()
        .join("log4j2.xml")
        .to_str()
        .expect(CONVERSION_FAILED_MESSAGE)
        .to_owned()
}

fn get_libpath_option() -> String {
    format!("-Djava.library.path={}", get_libpath())
}

fn get_libpath() -> String {
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

pub fn get_fake_service_artifact_path() -> String {
    java_binding_parent_root_dir()
        .join("fake-service/target/fake-service-artifact.jar")
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
    project_root_dir()
        .join("..")
        .canonicalize()
        .unwrap()
}

/// The path to `integration_tests` root directory.
fn project_root_dir() -> &'static Path {
    Path::new(env!("CARGO_MANIFEST_DIR"))
}

#[cfg(debug_assertions)]
fn target_path() -> &'static str {
    "target/debug"
}

#[cfg(not(debug_assertions))]
fn target_path() -> &'static str {
    "target/release"
}
