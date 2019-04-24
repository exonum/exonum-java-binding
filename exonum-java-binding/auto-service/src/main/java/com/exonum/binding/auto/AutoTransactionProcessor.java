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

import com.exonum.binding.annotations.AutoTransaction;
import com.exonum.binding.transaction.Transaction;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
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
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.exonum.binding.annotations.AutoTransaction")
public class AutoTransactionProcessor extends AbstractProcessor {

  private Messager messager;
  private Elements elementUtils;
  private Types typeUtils;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    messager = processingEnv.getMessager();
    elementUtils = processingEnv.getElementUtils();
    typeUtils = processingEnv.getTypeUtils();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    // Validate each annotation target
    Set<? extends Element> annotatedElements = roundEnv
        .getElementsAnnotatedWith(AutoTransaction.class);

    elementUtils.getAllAnnotationMirrors(annotatedElements.iterator().next()).stream()
        .filter(a -> a.a)
    // Check ids are unique
    //todo:

    List<TransactionFactory> transactionFactories = new ArrayList<>(annotatedElements.size());
    for (Element annotatedElement : annotatedElements) {
      // Must be a public static method with signature (byte[]) -> Transaction or subclass (todo: can we check accessibility to not require all to be 'public'?

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
        error(annotatedElement, "The factory method (%s) must be public static",
            methodElement.getSimpleName());
        return true;
      }

      // Check the signature
      // Check the return type
      TypeMirror returnType = methodElement.getReturnType();
      // fixme: Will it work? Or shall I use isAssignable? Or query class interfaces?
      TypeMirror transactionType = elementUtils.getTypeElement(Transaction.class.getName()).asType();
      if (!typeUtils.isSubtype(returnType, transactionType)) {
        error(methodElement, "The factory method (%s) must return a type"
                + "assignable to %s, but returns %s",
            methodElement.getSimpleName(),
            typeUtils.asElement(transactionType).getSimpleName(),
            typeUtils.asElement(returnType).getSimpleName());
        return true;
      }

      // Check the argument type
      List<? extends VariableElement> parameters = methodElement.getParameters();
      if (parameters.size() != 1) {
        // todo: better error reporting
        error(methodElement, "The method element must have a single argument of type byte[]");
        return true;
      }

      VariableElement methodParameter = parameters.get(0);
      ArrayType byteArrayType = typeUtils.getArrayType(
          typeUtils.getPrimitiveType(TypeKind.BYTE));
      if (!typeUtils.isSameType(methodParameter.asType(), byteArrayType)) {
        // todo: better error reporting
        error(methodParameter, "The method element must have a single argument of type byte[]");
        return true;
      }

      transactionFactories.add(new TransactionFactory(methodElement));
    }

    // Validate each annotation parameters: no duplicate ids.
    // fixme: it got too complex
    ImmutableListMultimap<Short, TransactionFactory> index = Multimaps
        .index(transactionFactories, f -> f.transac tionId);
    Map<Short, Collection<TransactionFactory>> factoriesWithDuplicateIds = Maps
        .filterValues(index.asMap(), factories -> factories.size() > 1);
    for (Entry<Short, Collection<TransactionFactory>> entry : factoriesWithDuplicateIds
        .entrySet()) {
      Short txId = entry.getKey();
      List<ExecutableElement> methods = entry.getValue().stream()
          .map(f -> f.factoryMethod)
          .collect(Collectors.toList());

      ExecutableElement firstMethod = methods.get(0);
      error(firstMethod, "Transaction id (%d) is duplicated in the following classes %s");
    }
    // Generate code
    return false;
  }

  private String formatDuplicateIds(Entry<Short, Collection<TransactionFactory>> e) {
    // You want classes, right? :trollface:
    return String.format("%d: %s", e.getKey(), e.getValue());
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

  private static class TransactionFactory {

    private final ExecutableElement factoryMethod;
    private final short transactionId;

    TransactionFactory(ExecutableElement factoryMethod) {
      this.factoryMethod = factoryMethod;
      this.transactionId = extractTxId(factoryMethod);
    }

    private static short extractTxId(ExecutableElement factoryMethod) {
      AutoTransaction annotation = factoryMethod.getAnnotation(AutoTransaction.class);
      return annotation.id();
    }
  }
}
