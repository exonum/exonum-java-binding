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

package com.exonum.binding.testkit;

import com.exonum.binding.util.LibraryLoader;
import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class TestKitExtension implements ParameterResolver {

  private static final Namespace NAMESPACE = Namespace.create(TestKitExtension.class);
  private static final String KEY = "ResourceKey";
  private static final Set<Class> testKitModificationAnnotations =
      ImmutableSet.of(Auditor.class, Validator.class, ValidatorCount.class,
          WithoutTimeService.class);

  static {
    LibraryLoader.load();
  }

  private final TestKit.Builder testKitBuilder;

  public TestKitExtension(TestKit.Builder testKitBuilder) {
    this.testKitBuilder = testKitBuilder;
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getType() == TestKit.class;
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    checkExtensionContext(extensionContext);
    CloseableTestKit closeableTestKit =
        getStore(extensionContext).get(KEY, CloseableTestKit.class);
    // Check if any TestKit configuration annotations are used
    boolean annotationsUsed = annotationsUsed(parameterContext);
    TestKit testKit;
    if (closeableTestKit == null) {
      if (annotationsUsed) {
        updateTestKitBuilder(parameterContext, extensionContext);
      }
      testKit = testKitBuilder.build();
      CloseableTestKit newCloseableTestKit = new CloseableTestKit(testKit);
      getStore(extensionContext).put(KEY, newCloseableTestKit);
    } else {
      // Throw an exception if TestKit was already instantiated in this context, but user tries to
      // reconfigure it
      if (annotationsUsed) {
        throw new RuntimeException("TestKit was parameterized with annotations after being"
            + " instantiated in " + extensionContext.getDisplayName());
      }
      testKit = closeableTestKit.getTestKit();
    }
    return testKit;
  }

  /**
   * Check the extension context and throw if TestKit is injected in @BeforeAll or @AfterAll.
   */
  private void checkExtensionContext(ExtensionContext extensionContext) {
    Optional<Method> testMethod = extensionContext.getTestMethod();
    testMethod.orElseThrow(() ->
        new RuntimeException("TestKit can't be injected in @BeforeAll or @AfterAll"));
  }

  private boolean annotationsUsed(ParameterContext parameterContext) {
    return testKitModificationAnnotations.stream()
        .anyMatch(annotation -> parameterContext.findAnnotation(annotation).isPresent());
  }

  private void updateTestKitBuilder(ParameterContext parameterContext,
                                    ExtensionContext extensionContext) {
    Optional<Auditor> auditorAnnotation = parameterContext.findAnnotation(Auditor.class);
    Optional<Validator> validatorAnnotation = parameterContext.findAnnotation(Validator.class);
    if (auditorAnnotation.isPresent() && validatorAnnotation.isPresent()) {
      throw new RuntimeException("Both @Validator and @Auditor annotations were used in "
          + extensionContext.getDisplayName());
    }
    auditorAnnotation.ifPresent(auditor -> testKitBuilder.withNodeType(EmulatedNodeType.AUDITOR));
    validatorAnnotation.ifPresent(validator ->
        testKitBuilder.withNodeType(EmulatedNodeType.VALIDATOR));

    Optional<ValidatorCount> validatorCountAnnotation =
        parameterContext.findAnnotation(ValidatorCount.class);
    validatorCountAnnotation.ifPresent(validatorCount ->
        testKitBuilder.withValidators(validatorCount.validatorCount()));

    Optional<WithoutTimeService> withTimeServiceAnnotation =
        parameterContext.findAnnotation(WithoutTimeService.class);
    withTimeServiceAnnotation.ifPresent(withoutTimeService ->
        testKitBuilder.withTimeService(null));
  }

  private Store getStore(ExtensionContext context) {
    return context.getStore(NAMESPACE);
  }

  private static class CloseableTestKit implements CloseableResource {

    private final TestKit testKit;

    CloseableTestKit(TestKit testKit) {
      this.testKit = testKit;
    }

    TestKit getTestKit() {
      return testKit;
    }

    @Override
    public void close() {
      testKit.disposeInternal();
    }
  }
}
