/*
 * Copyright 2018 The Exonum Team
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

package com.exonum.binding.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation intended to mark APIs that are good candidates to be generated auto-magically.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({
    ElementType.ANNOTATION_TYPE,
    ElementType.TYPE,
    ElementType.METHOD
})
public @interface AutoGenerationCandidate {
  /**
   * Why or how an element can be auto-generated.
   */
  String reason() default  "";
}
