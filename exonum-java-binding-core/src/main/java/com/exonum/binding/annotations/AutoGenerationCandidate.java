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
   * Why or how can an element be auto-generated?
   */
  String reason() default  "";
}
