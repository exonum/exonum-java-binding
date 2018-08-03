extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;
extern crate rand;

use integration_tests::vm::{create_vm_for_leak_tests, KIB, MIB};
use java_bindings::jni::objects::JObject;
use java_bindings::jni::JavaVM;
use rand::prelude::*;

const MEMORY_LIMIT_MIB: usize = 32;

lazy_static! {
    static ref JVM: JavaVM = create_vm_for_leak_tests(MEMORY_LIMIT_MIB);
}

/// Tests that a JVM does not leak the local references that exceed the capacity of the current
/// local frame.
#[test]
#[ignore]
fn jvm_must_not_leak_local_references_exceeding_frame_capacity() {
    // This test should totally allocate more memory than the limit is set,
    // but per iteration it should consume certainly less than the limit.
    // Allocated arrays filled with a random noise to avoid memory de-duplication tricks.
    // The test fails if local references are leaked.

    // Total (hard) heap size limit. You'll get OOME if happen to exceed that.
    let memory_limit: usize = MEMORY_LIMIT_MIB * MIB; // 32 MiB
    let local_frame_capacity = 32;
    // Allocate more references in each frame than its capacity.
    // If JVM leaks them when the frame is dropped, the test will crash.
    let references_per_frame = local_frame_capacity * 2; // 64
    let array_size = 256 * KIB;

    let exceeding_arrays_size_per_frame =
        array_size * (references_per_frame - local_frame_capacity);
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
                let _java_obj = env
                    .byte_array_from_slice(&array)
                    .expect("Can't create new local object.");
            }
            Ok(JObject::null())
        }).unwrap();
        println!(
            "Iteration {} complete. Total size of (potentially) leaked Java-allocated arrays: {} kiB",
            i + 1,
            (i + 1) * (exceeding_arrays_size_per_frame) / KIB
        );
    }
}
