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

package com.exonum.binding.core.storage.indices;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Disabled;

/**
 * Indicates that a test is an integration test of proof <em>verification</em>.
 * Such tests are temporarily disabled till the proof verification is fully implemented.
 *
 * <p>When it is implemented — reconsider both the ITs of proof <em>creation</em> and
 * <em>verification</em>.
 *
 * <p>See the epic https://jira.bf.local/browse/ECR-3784
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Disabled
public @interface DisabledProofTest {
}
