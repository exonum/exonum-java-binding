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

extern crate integration_tests;
extern crate java_bindings;
#[macro_use]
extern crate lazy_static;
extern crate rand;

use integration_tests::vm::{create_vm_for_leak_tests, KIB, MIB};
use java_bindings::jni::JavaVM;
use java_bindings::Executor;
use rand::prelude::*;

use std::sync::Arc;

const MEMORY_LIMIT_MIB: usize = 32;

lazy_static! {
    static ref JVM: Arc<JavaVM> = Arc::new(create_vm_for_leak_tests(MEMORY_LIMIT_MIB));
    static ref EXECUTOR: Executor = Executor::new(JVM.clone());
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

    // Total (hard) heap size limit. You'll get OOME if happen to exceed that.
    let memory_limit: usize = MEMORY_LIMIT_MIB * MIB; // 32 MiB
    let local_frame_capacity = 32;
    let array_size = 256 * KIB;

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
                    let _java_obj = env
                        .byte_array_from_slice(&array)
                        .expect("Can't create new local object.");
                }
                Ok(())
            })
            .unwrap();
        println!(
            "Iteration {} complete. Total size of Java-allocated arrays: {} kiB",
            i + 1,
            ((i + 1) * total_allocation_size_per_frame) / KIB
        );
    }
}
