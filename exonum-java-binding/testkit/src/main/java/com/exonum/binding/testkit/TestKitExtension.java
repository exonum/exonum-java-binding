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

/**
 * Extension that injects TestKit into service tests and destroys afterwards. Register this
 * extension with TestKit builder and a TestKit will be injected as a parameter, instantiated from
 * given builder. Note that this extension shouldn't be static. Example usage:
 *
 * <pre><code>
 * &#64;RegisterExtension
 * TestKitExtension testKitExtension = new TestKitExtension(
 *     TestKit.builder()
 *         .withService(TestServiceModule.class));
 *
 * &#64;BeforeEach
 * void setUp(TestKit testKit) {
 *   // Set up
 * }
 *
 * &#64;Test
 * void test(TestKit testKit) {
 *   // Test logic
 * }
 * </code></pre>
 * instead of:
 * <pre><code>
 * private TestKit testKit;
 *
 * &#64;BeforeEach
 * void setUp() {
 *   testKit = TestKit.forService(TestServiceModule.class));
 *   // Set up
 * }
 *
 * &#64;Test
 * void test() {
 *   // Test logic
 * }
 *
 * &#64;AfterEach
 * void destroyTestKit() {
 *   testKit.close();
 * }
 * </code></pre>
 *
 * <p>As different tests might need slightly different TestKit configuration, following
 * parameterization annotations are available:
 * <ul>
 *   <li>{@link Validator} sets main TestKit node type to validator</li>
 *   <li>{@link Auditor} sets main TestKit node type to auditor</li>
 *   <li>{@link ValidatorCount} sets number of validator nodes in the TestKit network</li>
 * </ul>
 * These annotations should be applied on TestKit parameter:
 * <pre><code>
 * &#64;RegisterExtension
 * TestKitExtension testKitExtension = new TestKitExtension(
 *     TestKit.builder()
 *         .withService(TestServiceModule.class));
 *
 * &#64;Test
 * void test(&#64;Auditor &#64;ValidatorCount(8) TestKit testKit) {
 *   // Test logic
 * }
 * </code></pre>
 *
 * <p>Note that after TestKit is instantiated in given test context, it is not possible to
 * reconfigure it again. For example, if TestKit is injected in @BeforeEach method, it can't be
 * reconfigured in @Test or @AfterEach methods.
 *
 * <p>Also note that TestKit can't be injected in @BeforeAll and @AfterAll methods.
 */
public class TestKitExtension implements ParameterResolver {

  private static final Namespace NAMESPACE = Namespace.create(TestKitExtension.class);
  private static final String TESTKIT_KEY = "Testkit";
  private static final Set<Class> testKitModificationAnnotations =
      ImmutableSet.of(Auditor.class, Validator.class, ValidatorCount.class);

  private final TestKit.Builder templateTestKitBuilder;

  public TestKitExtension(TestKit.Builder templateTestKitBuilder) {
    this.templateTestKitBuilder = templateTestKitBuilder;
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType() == TestKit.class;
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    checkExtensionContext(extensionContext);
    CloseableTestKit closeableTestKit =
        getStore(extensionContext).get(TESTKIT_KEY, CloseableTestKit.class);
    TestKit testKit;
    if (closeableTestKit == null) {
      testKit = buildTestKit(parameterContext, extensionContext);
      getStore(extensionContext).put(TESTKIT_KEY, new CloseableTestKit(testKit));
    } else {
      // Throw an exception if TestKit was already instantiated in this context, but user tries to
      // reconfigure it
      if (annotationsUsed(parameterContext)) {
        throw new ParameterResolutionException("TestKit was parameterized with annotations after"
            + " being instantiated in " + extensionContext.getDisplayName());
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
        new ParameterResolutionException("TestKit can't be injected in @BeforeAll or @AfterAll"
            + " because it is a stateful, mutable object and sharing it between all tests is"
            + " error-prone. Consider injecting it in @BeforeEach instead.\n"
            + " If you do need the same instance for all tests — just use `TestKit#builder`"
            + " directly. Don't forget to destroy it in @AfterEach."));
  }

  private TestKit buildTestKit(ParameterContext parameterContext,
                               ExtensionContext extensionContext) {
    TestKit.Builder testKitBuilder = createTestKitBuilder(parameterContext, extensionContext);
    return testKitBuilder.build();
  }

  private boolean annotationsUsed(ParameterContext parameterContext) {
    return testKitModificationAnnotations.stream()
        .anyMatch(parameterContext::isAnnotated);
  }

  private TestKit.Builder createTestKitBuilder(ParameterContext parameterContext,
                                               ExtensionContext extensionContext) {
    TestKit.Builder testKitBuilder = templateTestKitBuilder.copy();
    Optional<Auditor> auditorAnnotation = parameterContext.findAnnotation(Auditor.class);
    Optional<Validator> validatorAnnotation = parameterContext.findAnnotation(Validator.class);
    if (auditorAnnotation.isPresent() && validatorAnnotation.isPresent()) {
      throw new ParameterResolutionException("Both @Validator and @Auditor annotations were used"
          + " in " + extensionContext.getDisplayName());
    }
    auditorAnnotation.ifPresent(auditor -> testKitBuilder.withNodeType(EmulatedNodeType.AUDITOR));
    validatorAnnotation.ifPresent(validator ->
        testKitBuilder.withNodeType(EmulatedNodeType.VALIDATOR));

    Optional<ValidatorCount> validatorCountAnnotation =
        parameterContext.findAnnotation(ValidatorCount.class);
    validatorCountAnnotation.ifPresent(validatorCount ->
        testKitBuilder.withValidators(validatorCount.value()));
    return testKitBuilder;
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
      testKit.close();
    }
  }
}
