extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;
extern crate rand;

use java_bindings::{JniExecutor, MainExecutor};
use java_bindings::jni::{InitArgsBuilder, JNIVersion, JavaVM};
use java_bindings::jni::objects::JObject;
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

    let local_frame_capacity = 32;
    let references_per_frame = local_frame_capacity * 2; // 64

    EXECUTOR
        .with_attached_capacity(local_frame_capacity as i32, |env| {
            // Create N Java strings, keep the local references.
            let mut java_strings = Vec::new();
            for i in 0..references_per_frame {
                print!("Try: {}; limit: {}. ", i + 1, local_frame_capacity);
                let java_string = env.new_string(expected_string_at(i)).expect(
                    "Can't create new local object.",
                );
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

/// Tests that an implementation of the Executor does not leak the local references
/// created in the lambda passed to `with_attached_capacity`.
///
/// The natural way to achieve that in an Executor implementation is to allocate a
/// local reference frame for each passed lambda.
#[test]
fn executor_must_not_leak_local_references() {
    // This test should totally allocate more memory than the limit is set,
    // but per iteration it should consume certainly less than the limit.
    // Allocated arrays filled with a random noise to avoid memory de-duplication tricks.
    // The test fails if local references are leaked.

    let mib = 1024 * 1024;
    // Total (hard) heap size limit. You'll get OOME if happen to exceed that.
    let memory_limit: usize = MEMORY_LIMIT_MB * mib; // 128 MiB
    let local_frame_capacity = 32;
    let array_size = 1 * mib; // 1 MiB

    let iterations_to_exceed_the_limit = 1 + (memory_limit / (local_frame_capacity * array_size));

    println!("Iterations: {}", iterations_to_exceed_the_limit);

    // Double-check we picked the constants properly.
    let total_allocation_size_per_frame = local_frame_capacity * array_size;
    assert!(total_allocation_size_per_frame <= memory_limit / 2);

    let mut array = vec![0_u8; array_size];

    for i in 0..iterations_to_exceed_the_limit {
        EXECUTOR
            .with_attached_capacity(local_frame_capacity as i32, |env| {
                // Create and leak 'local_frame_capacity' Java arrays.
                //
                // If an executor does not perform this lambda in a separate local reference frame,
                // these Java objects will leak until the current thread is stopped.
                for _ in 0..local_frame_capacity {
                    thread_rng().fill(&mut array[..]);
                    let _java_obj = env.byte_array_from_slice(&array).expect(
                        "Can't create new local object.",
                    );
                }
                Ok(())
            })
            .unwrap();
        println!(
            "Iteration {} complete. Total size of Java-allocated arrays: {} kiB",
            i,
            ((i + 1) * total_allocation_size_per_frame) / 1024
        );
    }
}

/// Tests that a JVM does not leak the local references that exceed the capacity of the current
/// local frame.
#[test]
fn jvm_must_not_leak_local_references_exceeding_frame_capacity() {
    // This test should totally allocate more memory than the limit is set,
    // but per iteration it should consume certainly less than the limit.
    // Allocated arrays filled with a random noise to avoid memory de-duplication tricks.
    // The test fails if local references are leaked.

    let mib = 1024 * 1024;
    // Total (hard) heap size limit. You'll get OOME if happen to exceed that.
    let memory_limit: usize = MEMORY_LIMIT_MB * mib; // 128 MiB
    let local_frame_capacity = 32;
    // Allocate more references in each frame than its capacity.
    // If JVM leaks them when the frame is dropped, the test will crash.
    let references_per_frame = local_frame_capacity * 2; // 64
    let array_size = 1 * mib;

    let exceeding_arrays_size_per_frame = array_size *
        (references_per_frame - local_frame_capacity);
    let iterations_to_exceed_the_limit = 1 + (memory_limit / exceeding_arrays_size_per_frame);

    // Double-check we picked the constants properly.
    let total_allocation_size_per_frame = references_per_frame * array_size;
    assert!(total_allocation_size_per_frame <= memory_limit / 2);

    println!("Test iterations: {}", iterations_to_exceed_the_limit);

    let mut array = vec![0_u8; array_size];

    let env = JVM.attach_current_thread().unwrap();

    for i in 0..iterations_to_exceed_the_limit {
        env.with_local_frame(local_frame_capacity as i32, || {
            // Create and leak 'references_per_frame' Java arrays.
            for _ in 0..references_per_frame {
                thread_rng().fill(&mut array[..]);
                let _java_obj = env.byte_array_from_slice(&array).expect(
                    "Can't create new local object.",
                );
            }
            Ok(JObject::null())
        }).unwrap();
        println!(
            "Iteration {} complete. Total size of (potentially) leaked Java-allocated arrays: {} kiB",
            i,
            (i + 1) * (exceeding_arrays_size_per_frame) / 1024
        );
    }
}

const MEMORY_LIMIT_MB: usize = 128;

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
