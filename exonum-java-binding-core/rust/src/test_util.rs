use jni::{InitArgsBuilder, JNIVersion, JavaVM};

/// Creates a configured instance of `JavaVM`.
/// _This function should be called only *once*._
pub fn create_vm(debug: bool) -> JavaVM {
    let mut jvm_args_builder = InitArgsBuilder::new()
        .version(JNIVersion::V8);

    if debug {
        jvm_args_builder = jvm_args_builder
            .option("-Xcheck:jni")
            .option("-Xdebug");
    }

    let jvm_args = jvm_args_builder
        .build()
        .unwrap_or_else(|e| panic!(format!("{:#?}", e)));

    JavaVM::new(jvm_args)
        .unwrap_or_else(|e| panic!(format!("{:#?}", e)))
}
