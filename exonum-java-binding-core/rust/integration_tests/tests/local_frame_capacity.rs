extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;
extern crate rand;

use integration_tests::vm::create_vm_for_leak_tests;
use java_bindings::jni::JavaVM;
use java_bindings::{JniExecutor, MainExecutor};

use std::sync::Arc;

const MEMORY_LIMIT_MIB: usize = 32;

lazy_static! {
    static ref JVM: Arc<JavaVM> = Arc::new(create_vm_for_leak_tests(MEMORY_LIMIT_MIB));
    static ref EXECUTOR: MainExecutor = MainExecutor::new(JVM.clone());
}

#[test]
fn local_frame_allows_overflow() {
    // This test allocates a local frame with a default number of entries (32)
    // and tries to overflow it with a big number of local references.
    // Test fails if JVM doesn't allow to allocate big number of local references
    // over the requested frame capacity.

    let local_frame_capacity = 32;
    let references_per_frame = local_frame_capacity * 2; // 64

    EXECUTOR
        .with_attached_capacity(local_frame_capacity as i32, |env| {
            // Create N Java strings, keep the local references.
            let mut java_strings = Vec::new();
            for i in 0..references_per_frame {
                print!("Try: {}; limit: {}. ", i + 1, local_frame_capacity);
                let java_string = env.new_string(expected_string_at(i))
                    .expect("Can't create new local object.");
                java_strings.push(java_string);
                println!(" Ok.");
            }
            // Check all the references can be used to access the corresponding Java string.
            for (i, java_string_ref) in java_strings.into_iter().enumerate() {
                let java_string: String = env.get_string(java_string_ref)
                    .expect("Can't get object.")
                    .into();
                assert_eq!(java_string, expected_string_at(i));
            }
            Ok(())
        })
        .unwrap();
}

fn expected_string_at(index: usize) -> String {
    format!("s{}", index)
}
