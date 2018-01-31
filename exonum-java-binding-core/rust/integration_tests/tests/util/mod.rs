use jni::{InitArgsBuilder, JNIVersion, JavaVM};

use std::fs::File;
use std::io::{self, Read};
use std::path::Path;

/// Creates a configured instance of `JavaVM`.
/// _This function should be called only *once*._
pub fn create_vm(debug: bool) -> JavaVM {
    let classpath = get_classpath_option().unwrap();

    let mut jvm_args_builder = InitArgsBuilder::new()
        .version(JNIVersion::V8)
        .option(&classpath);

    if debug {
        jvm_args_builder = jvm_args_builder.option("-Xcheck:jni").option("-Xdebug");
    }

    let jvm_args = jvm_args_builder.build().unwrap_or_else(
        |e| panic!(format!("{:#?}", e)),
    );

    JavaVM::new(jvm_args).unwrap_or_else(|e| panic!(format!("{:#?}", e)))
}

fn get_classpath_option() -> io::Result<String> {
    // FIXME здесь реализована безусловная зависимость от наличия файла classpath.txt,
    // нужно ли это или у нас возможна ситуация, когда мы можем обойтись без этих классов
    // например, для бенчмарков?

    // The current os path is expected to be
    // `<exonum-java-bindings project root>/exonum-java-binding-core/rust/integration_tests/`
    let path = "../../../exonum-java-binding-fakes/target/ejb-fakes-classpath.txt";
    let mut class_path = String::new();
    File::open(path)?.read_to_string(&mut class_path)?;
    let fakes_path = Path::new("../../../exonum-java-binding-fakes/target/classes/")
        .canonicalize()?;
    // We don't support the case when OsStr can't be converted into utf-8
    let fakes_classes = fakes_path.to_str().unwrap();
    // FIXME вполне возможна ситуация, когда пути будут содержать как минимум пробелы (например,
    // на машине разработчика из-за имени пользователя), всё ли ок у нас с экранированием?
    // FIXME should be used `;` as path separator on Windows?
    Ok(format!("-Djava.class.path={}:{}", class_path, fakes_classes))
}
