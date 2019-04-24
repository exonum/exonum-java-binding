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

package com.exonum.binding.auto;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.annotations.AutoTransaction;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.Transaction;
import com.google.auto.service.AutoService;
import com.google.common.collect.Maps;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.exonum.binding.annotations.AutoTransaction")
public final class AutoTransactionProcessor extends AbstractProcessor {

  private Messager messager;
  private Elements elementUtils;
  private Types typeUtils;
  private Filer filer;
  private boolean outputWritten = false;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    filer = processingEnv.getFiler();
    messager = processingEnv.getMessager();
    elementUtils = processingEnv.getElementUtils();
    typeUtils = processingEnv.getTypeUtils();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (outputWritten) {
      return false;
    }

    // Validate each annotation target
    Set<? extends Element> annotatedElements = roundEnv
        .getElementsAnnotatedWith(AutoTransaction.class);

    // Validate each annotation parameter: check no duplicate ids
    Map<Short, Element> elementsById = new TreeMap<>();
    for (Element annotatedElement : annotatedElements) {
      short id = extractTxId(annotatedElement);
      // todo: how to find the annotation _element_ on the annotated element?

      if (elementsById.containsKey(id)) {
        Element firstAnnotated = elementsById.get(id);
        error(annotatedElement, "Element %s has the same transaction id (%s) as element %s",
            annotatedElement, id, firstAnnotated);
        error(firstAnnotated,  "    first declared on %s", firstAnnotated);
        return true;
      }

      elementsById.put(id, annotatedElement);
    }

    for (Element annotatedElement : annotatedElements) {
      messager.printMessage(Kind.NOTE,
          String.format("Discovered @AutoTransaction on %s", annotatedElement), annotatedElement);

      // Must be a public static method with signature (RawTransaction) -> Transaction or subclass
      // todo: can we check accessibility to not require all to be 'public' (from where?)?
      //    Also see effective visibility code in Auto (it won't solve all problems, but some):
      //    com.google.auto.common.Visibility.effectiveVisibilityOfElement
      // Check it is a method
      if (annotatedElement.getKind() != ElementKind.METHOD) {
        error(annotatedElement, "%s annotation is allowed on methods only",
            AutoTransaction.class.getSimpleName());
        return true;
      }
      ExecutableElement methodElement = (ExecutableElement) annotatedElement;

      // Check the method is 'public static'
      Set<Modifier> modifiers = methodElement.getModifiers();
      if (!modifiers.containsAll(EnumSet.of(Modifier.PUBLIC, Modifier.STATIC))) {
        // todo: better error reporting (actual/expected)
        error(methodElement, "The factory method (%s) must be public static",
            methodElement);
        return true;
      }

      // Check the signature
      // Check the return type
      TypeMirror returnType = methodElement.getReturnType();
      TypeMirror transactionType = typeMirrorOf(Transaction.class);
      if (!typeUtils.isAssignable(returnType, transactionType)) {
        error(methodElement, "The factory method (%s) must return a type"
                + "assignable to %s, but returns %s",
            methodElement,
            typeUtils.asElement(transactionType),
            typeUtils.asElement(returnType));
        return true;
      }

      // Check the argument type
      List<? extends VariableElement> parameters = methodElement.getParameters();
      if (parameters.size() != 1) {
        // todo: better error reporting
        TypeMirror rawTxType = typeMirrorOf(RawTransaction.class);
        error(methodElement, "The method (%s) must have a single argument of type %s",
            methodElement, rawTxType);
        return true;
      }

      VariableElement methodParameter = parameters.get(0);
      TypeMirror rawTxType = typeMirrorOf(RawTransaction.class);
      if (!typeUtils.isSameType(methodParameter.asType(), rawTxType)) {
        // todo: better error reporting
        error(methodParameter, "The method element must have a single argument of type %s",
            rawTxType);
        return true;
      }
    }

    Map<Short, ExecutableElement> txFactories = Maps.transformValues(elementsById,
        ExecutableElement.class::cast);

    // Generate code
    if (!txFactories.isEmpty()) {
      try {
        AutoTransactionConverterWriter writer = new AutoTransactionConverterWriter(txFactories, filer);
        writer.write();
      } catch (IOException e) {
        messager.printMessage(Kind.ERROR, e.getMessage());
        return true;
      }

      // todo: improve this
      outputWritten = true;
    }
    return true;
  }

  private static short extractTxId(Element annotatedElement) {
    AutoTransaction annotation = annotatedElement.getAnnotation(AutoTransaction.class);
    checkNotNull(annotation, "%s does not have %s present", annotatedElement, annotation);
    return annotation.id();
  }

  private TypeMirror typeMirrorOf(Class<?> type) {
    return elementUtils.getTypeElement(type.getCanonicalName())
        .asType();
  }


  @FormatMethod
  private void error(Element e, @FormatString String message, Object... arguments) {
    String formattedMessage = String.format(message, arguments);
    messager.printMessage(Kind.ERROR, formattedMessage, e);
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }
}
