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

import com.exonum.binding.service.TransactionConverter;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.Transaction;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

final class AutoTransactionConverterWriter {

  private final Filer filer;
  private final Map<Short, ExecutableElement> txFactories;

  AutoTransactionConverterWriter(Map<Short, ExecutableElement> txFactories, Filer filer) {
    this.filer = filer;
    this.txFactories = txFactories;
  }

  void write() throws IOException {
    ParameterSpec rawTransactionParam = ParameterSpec
        .builder(RawTransaction.class, "rawTransaction")
        .build();

    MethodSpec toTransactionMethod = MethodSpec.methodBuilder("toTransaction")
        .addAnnotation(Override.class)
        .addModifiers(Modifier.PUBLIC)
        .addParameter(rawTransactionParam)
        .returns(Transaction.class)
        .addCode(createToTransactionBody(rawTransactionParam))
        .build();

    TypeSpec converterClass = TypeSpec.classBuilder("AutoTransactionConverter")
        // todo: com.google.auto.common.GeneratedAnnotationSpecs.generatedAnnotationSpec(javax.lang.model.util.Elements, java.lang.Class<?>, java.lang.String)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addSuperinterface(TransactionConverter.class)
        .addMethod(toTransactionMethod)
        .build();

    // todo: determine (somehow somewhere) package — the algorithm must be deterministic and simple
    //  for users to understand
    JavaFile file = JavaFile.builder("com.example.auto", converterClass)
        .build();

    file.writeTo(filer);
  }

  private CodeBlock createToTransactionBody(ParameterSpec rawTransaction) {
    String txIdVar = "txId";
    CodeBlock.Builder builder = CodeBlock.builder()
        .addStatement("short $N = $N.getTransactionId()", txIdVar, rawTransaction);

    // Begin switch statement
    builder.beginControlFlow("switch ($N)", txIdVar);

    // Add "case txId:" statements
    for (Entry<Short, ExecutableElement> e : txFactories.entrySet()) {
      short txId = e.getKey();
      ExecutableElement factoryMethod = e.getValue();
      TypeElement methodClass = getContainingClass(factoryMethod);

      builder.addStatement("case $L: return $T.$N($N)", txId, methodClass,
          factoryMethod.getSimpleName(), rawTransaction);
    }

    // Add "default:" statement (if txId is unknown)
    builder.beginControlFlow("default:")
        .addStatement("String errorMessage = String.format($S, $N, $N)",
            "Unknown transaction id (%d) in %s", txIdVar, rawTransaction)
        .addStatement("throw new $T($N)", IllegalArgumentException.class, "errorMessage")
        .endControlFlow();

    // End switch statement
    builder.endControlFlow();

    return builder.build();
  }

  private TypeElement getContainingClass(ExecutableElement factoryMethod) {
    Element enclosingElement = factoryMethod.getEnclosingElement();
    ElementKind kind = enclosingElement.getKind();
    if (!(kind.isClass() || kind.isInterface())) {
      throw new AssertionError(String.format("Unexpected enclosing element of %s", factoryMethod));
    }
    return (TypeElement) enclosingElement;
  }

}
