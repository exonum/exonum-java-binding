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

package com.exonum.binding.core.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a documentation of a field, method or class needs to be improved
 * (or added if absent).
 *
 * <p>Example usages:
 * <ul>
 *   <li>Certain aspects are under-documented or not-yet-known and need to be improved
 *       in the future.</li>
 *   <li>You need to urgently integrate a patch, but have not upgraded the documentation properly,
 *       and will do that ASAP. Such annotation is preferred to crude, inadequate comments.</li>
 *   <li>A badly documented code got into the project, and you had to dig deeply
 *       into implementation details to understand how to use it.</li>
 * </ul>
 *
 * <p>Do not leave this task unassigned!
 *
 * <p>It also works as a waiver for the CI server. Do not abuse.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({
    ElementType.ANNOTATION_TYPE,
    ElementType.TYPE,
    ElementType.FIELD,
    ElementType.METHOD,
    ElementType.CONSTRUCTOR,
    ElementType.PARAMETER,
})
public @interface ImproveDocs {
  /**
   * Whom this item is assigned to.
   */
  String assignee();

  /**
   * A description of why this annotation was added.
   */
  String reason() default "";
}
