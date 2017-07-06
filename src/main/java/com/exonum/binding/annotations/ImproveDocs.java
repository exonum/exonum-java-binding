package com.exonum.binding.annotations;

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
   * @return whom this item is assigned to.
   */
  String assignee();

  /**
   * @return a description of why this annotation was added.
   */
  String reason() default "";
}
