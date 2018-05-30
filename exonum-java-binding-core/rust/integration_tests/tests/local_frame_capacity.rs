extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;
extern crate rand;

use java_bindings::{JniExecutor, MainExecutor};
use java_bindings::jni::JavaVM;
use integration_tests::vm::create_vm_for_tests;
use rand::prelude::*;

lazy_static! {
    static ref JVM: JavaVM = create_vm_for_tests();
    static ref EXECUTOR: MainExecutor = MainExecutor::new(&JVM);
}

#[test]
fn local_frame_allows_overflow() {
    // This test allocates a local frame with a default number of entries (32)
    // and tries to overflow it with a big number of local references.
    // Test fails if JVM doesn't allow to allocate big number of local references
    // over the requested frame capacity.

    const BIG_NUMBER: i32 = 65536;

    EXECUTOR.with_attached(|env| {
        let mut strings = Vec::new();
        for i in 1..BIG_NUMBER + 1 {
            print!("Try: {}; limit: {}. ", i, MainExecutor::LOCAL_FRAME_CAPACITY);
            let java_string = env.new_string(format!("{}", i)).expect(
                "Can't create new local object.",
            );
            strings.push(java_string);
            println!(" Ok.");
        }
        for (i, java_string) in strings.into_iter().enumerate() {
            let java_string: String = env.get_string(java_string)
                .expect("Can't get object.")
                .into();
            let number_string = format!("{}", i + 1);
            assert_eq!(java_string, number_string);
        }
        Ok(())
    }).unwrap();
}

#[test]
fn local_references_doesnt_leak() {
    // This test should totally use 8 * 256 MiB = 2 GiB, but needs only 256 MiB at once.
    // -Xmx sets the heap limit as 1 GiB for tests.
    // Allocated arrays filled with a random noise to avoid memory deduplication tricks.
    // The test fails if local references are leaked.

    const ITER_NUM: usize = 8;
    const ARRAY_NUM: usize = 256;
    const ARRAY_SIZE: usize = 1024 * 1024;

    let mut big_array = vec![0_u8; ARRAY_SIZE];

    EXECUTOR.with_attached(|_env| {
        for n in 1..ITER_NUM + 1 {
            EXECUTOR.with_attached(|env| {
                for _ in 1..ARRAY_NUM + 1 {
                    thread_rng().fill(&mut big_array[..]);
                    let _java_obj = env.byte_array_from_slice(&big_array).expect(
                        "Can't create new local object.",
                    );
                }
                Ok(())
            }).unwrap();
            println!("Iteration {} complete. Totally allocated: {} MiB", n, n * ARRAY_NUM);
        };
        Ok(())
    }).unwrap();
}
