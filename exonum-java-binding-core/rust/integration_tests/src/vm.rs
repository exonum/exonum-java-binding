use java_bindings::jni::{InitArgsBuilder, JNIVersion, JavaVM};

use std::fs::File;
use std::io::Read;
use std::path::{Path, PathBuf};

/// Creates a configured `JavaVM` for benchmarks.
/// _`JavaVM` should be created only *once*._
#[allow(dead_code)]
pub fn create_vm_for_benchmarks() -> JavaVM {
    create_vm(false, false)
}

/// Creates a configured `JavaVM` for tests.
/// _`JavaVM` should be created only *once*._
#[allow(dead_code)]
pub fn create_vm_for_tests() -> JavaVM {
    create_vm(true, false)
}

/// Creates a configured `JavaVM` for tests with fake classes.
/// _`JavaVM` should be created only *once*._
#[allow(dead_code)]
pub fn create_vm_for_tests_with_fake_classes() -> JavaVM {
    create_vm(true, true)
}

/// Creates a configured `JavaVM`.
/// _JavaVM should be created only *once*._
fn create_vm(debug: bool, with_fakes: bool) -> JavaVM {
    let mut jvm_args_builder = InitArgsBuilder::new().version(JNIVersion::V8).option(
        &get_libpath_option(),
    );

    if with_fakes {
        jvm_args_builder = jvm_args_builder.option(&get_classpath_option());
    }
    if debug {
        jvm_args_builder = jvm_args_builder.option("-Xcheck:jni").option("-Xdebug");
    }

    let jvm_args = jvm_args_builder.build().unwrap_or_else(
        |e| panic!("{:#?}", e),
    );

    JavaVM::new(jvm_args).unwrap_or_else(|e| panic!("{:#?}", e))
}

fn get_libpath_option() -> String {
    let library_path = rust_project_root_dir()
        .join(target_path())
        .canonicalize()
        .expect(
            "Target path not found, but there should be \
            the libjava_bindings dynamically loading library",
        );
    let library_path = library_path.to_str().expect(
        "Failed to convert FS path into utf-8",
    );

    format!("-Djava.library.path={}", library_path)
}

fn rust_project_root_dir() -> PathBuf {
    Path::new(env!("CARGO_MANIFEST_DIR"))
        .join("..")
        .canonicalize()
        .unwrap()
}

fn project_root_dir() -> PathBuf {
    rust_project_root_dir()
        .join("../..")
        .canonicalize()
        .unwrap()
}

#[cfg(debug_assertions)]
fn target_path() -> &'static str {
    "target/debug"
}

#[cfg(not(debug_assertions))]
fn target_path() -> &'static str {
    "target/release"
}

fn get_classpath_option() -> String {
    let classpath_txt_path =
        project_root_dir().join("exonum-java-binding-fakes/target/ejb-fakes-classpath.txt");

    let mut class_path = String::new();
    File::open(classpath_txt_path)
        .expect("Can't open classpath.txt")
        .read_to_string(&mut class_path)
        .expect("Failed to read classpath.txt");

    let fakes_path = project_root_dir().join("exonum-java-binding-fakes/target/classes/");
    let fakes_classes = fakes_path.to_str().expect(
        "Failed to convert FS path into utf-8",
    );

    // should be used `;` as path separator on Windows [https://jira.bf.local/browse/ECR-587]
    format!("-Djava.class.path={}:{}", class_path, fakes_classes)
}
