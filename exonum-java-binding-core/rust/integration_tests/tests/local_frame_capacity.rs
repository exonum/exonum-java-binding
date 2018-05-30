extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;
extern crate rand;

use java_bindings::{JniExecutor, MainExecutor};
use java_bindings::jni::{InitArgsBuilder, JNIVersion, JavaVM};
use integration_tests::vm::get_libpath_option;
use rand::prelude::*;

lazy_static! {
    static ref JVM: JavaVM = create_vm();
    static ref EXECUTOR: MainExecutor = MainExecutor::new(&JVM);
}

#[test]
fn local_frame_allows_overflow() {
    // This test allocates a local frame with a default number of entries (32)
    // and tries to overflow it with a big number of local references.
    // Test fails if JVM doesn't allow to allocate big number of local references
    // over the requested frame capacity.

    let requested_frame_capacity = 32;
    let real_frame_capacity = requested_frame_capacity + 32; // 64
    let references_per_frame = real_frame_capacity * 2; // 128

    EXECUTOR
        .with_attached_capacity(requested_frame_capacity as i32, |env| {
            let mut strings = Vec::new();
            for i in 1..references_per_frame + 1 {
                print!("Try: {}; real limit: {}. ", i, real_frame_capacity);
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
        })
        .unwrap();
}

#[test]
fn local_references_doesnt_leak() {
    // This test should totally allocate more memory than the limit is set,
    // but per iteration it should consume certainly less than the limit.
    // Allocated arrays filled with a random noise to avoid memory deduplication tricks.
    // The test fails if local references are leaked.

    let mib = 1024 * 1024;
    // Total (hard) heap size limit. You'll get OOME if happen to exceed that.
    let memory_limit: usize = MEMORY_LIMIT_MB * mib; // 128 MiB
    let requested_frame_capacity = 32;
    let real_frame_capacity = requested_frame_capacity + 32; // 64
    let references_per_frame = real_frame_capacity * 2; // 128
    let array_size = memory_limit / (2 * references_per_frame); // 1 MiB

    let iterations_to_exceed_the_limit = memory_limit /
        ((references_per_frame - real_frame_capacity) * array_size);

    // Double-check we picked the constants properly.
    let total_allocation_size_per_frame = references_per_frame * array_size;
    assert!(total_allocation_size_per_frame <= memory_limit / 2);

    let mut big_array = vec![0_u8; array_size];

    EXECUTOR
        // Attached twice to avoid detachment during test.
        .with_attached(|_env| {
            for n in 1..iterations_to_exceed_the_limit + 1 {
                EXECUTOR
                    .with_attached_capacity(requested_frame_capacity as i32, |env| {
                        for _ in 0..references_per_frame {
                            thread_rng().fill(&mut big_array[..]);
                            let _java_obj = env.byte_array_from_slice(&big_array).expect(
                                "Can't create new local object.",
                            );
                        }
                        Ok(())
                    })
                    .unwrap();
                println!(
                    "Iteration {} complete. Potentially not deallocated: {} kiB",
                    n,
                    n * (references_per_frame - real_frame_capacity) * array_size / 1024
                );
            }
            Ok(())
        })
        .unwrap();
}

const MEMORY_LIMIT_MB: usize = 1;

fn create_vm() -> JavaVM {
    let jvm_args = InitArgsBuilder::new()
        .version(JNIVersion::V8)
        .option(&get_libpath_option())
        .option("-Xcheck:jni")
        .option("-Xdebug")
        .option(&format!("-Xmx{}m", MEMORY_LIMIT_MB))
        .build()
        .unwrap_or_else(|e| panic!("{:#?}", e));

    JavaVM::new(jvm_args).unwrap_or_else(|e| panic!("{:#?}", e))
}
