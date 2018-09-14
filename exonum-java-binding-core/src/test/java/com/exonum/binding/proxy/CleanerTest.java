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

package com.exonum.binding.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.exonum.binding.testutils.LoggingTestUtils;
import com.google.common.testing.NullPointerTester;
import java.util.List;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;

public class CleanerTest {

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  private ListAppender logAppender;

  private Cleaner context;

  @Before
  public void setUp() {
    logAppender = LoggingTestUtils.getCapturingLogAppender();

    context = new Cleaner();
  }

  @After
  public void tearDown() {
    logAppender.clear();
  }

  @Test
  public void testRejectsNull() {
    NullPointerTester tester = new NullPointerTester();
    tester.testAllPublicInstanceMethods(context);
  }

  @Test
  public void addActionToClosedExecutesAction() throws CloseFailuresException {
    context.close();

    CleanAction action = mock(CleanAction.class);

    try {
      context.add(action);
      fail("closed context ^ must throw");
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).startsWith("Cannot register a clean action");

      // Verify that the action was executed once.
      verify(action).clean();
    }
  }

  @Test
  public void addThrowingActionExceptionIncludesSuppressed() throws CloseFailuresException {
    context.close();

    // Create a throwing action.
    CleanAction action = mock(CleanAction.class);
    doThrow(RuntimeException.class).when(action).clean();

    try {
      context.add(action);
      fail("closed context ^ must throw");
    } catch (IllegalStateException e) {
      // Check the suppressed exceptions are properly initialized.
      Throwable[] suppressed = e.getSuppressed();
      assertThat(suppressed).hasSize(1);
      assertThat(suppressed[0]).isInstanceOf(RuntimeException.class);
    }
  }

  @Test
  public void closeEmptyNoExceptions() throws CloseFailuresException {
    context.close();
  }

  @Test
  public void closeOneAction() throws CloseFailuresException {
    CleanAction action = mock(CleanAction.class);

    context.add(action);

    context.close();

    verify(action).clean();
  }

  @Test
  public void closeOneActionLogsFailures() {
    CleanAction action = mock(CleanAction.class);
    doThrow(RuntimeException.class).when(action).clean();

    context.add(action);

    try {
      context.close();
      fail("context.close() must throw");
    } catch (CloseFailuresException expected) {
      List<String> logEvents = logAppender.getMessages();

      assertThat(logEvents).hasSize(1);

      assertThat(logEvents.get(0))
          .contains("ERROR")
          .contains("Exception occurred when this context (" + context
              + ") attempted to perform a clean operation (" + action);
    }
  }

  @Test
  public void closeMultipleActions() throws CloseFailuresException {
    CleanAction a1 = mock(CleanAction.class);
    CleanAction a2 = mock(CleanAction.class);

    context.add(a1);
    context.add(a2);

    context.close();

    // Verify that the proxies are closed in the reversed order they were added.
    InOrder inOrder = inOrder(a2, a1);
    inOrder.verify(a2).clean();
    inOrder.verify(a1).clean();
  }

  @Test
  public void closeMultipleActionsWhenFirstToBeClosedFails() {
    CleanAction a1 = mock(CleanAction.class);
    CleanAction a2 = mock(CleanAction.class);
    doThrow(RuntimeException.class).when(a2).clean();

    context.add(a1);
    context.add(a2);

    try {
      context.close();
      fail("Context must report that it failed to close a2");
    } catch (CloseFailuresException e) {
      // Verify that a1 was closed.
      verify(a1).clean();

      // Check the error message.
      assertThat(e).hasMessageStartingWith("1 exception(s) occurred when closing this context");
      // Check suppressed exceptions.
      Throwable[] suppressedExceptions = e.getSuppressed();
      assertThat(suppressedExceptions).hasSize(1);
      assertThat(suppressedExceptions[0]).isInstanceOf(RuntimeException.class);
    }
  }

  @Test
  public void closeIsIdempotent() throws CloseFailuresException {
    CleanAction action = mock(CleanAction.class);

    context.add(action);

    // First close must perform the clean action.
    context.close();
    // Second close must not have any effects.
    context.close();

    // Verify that the action was performed exactly once.
    verify(action).clean();
  }

  @Test
  public void toStringIncludesContextInformation() {
    String r = context.toString();

    assertThat(r).contains("hash");
    assertThat(r).contains("numRegisteredActions=0");
    assertThat(r).contains("closed=false");
  }

  @Test
  public void toStringWithDescriptionIncludesContextInformation() {
    String description = "Transaction#execute";
    context = new Cleaner(description);

    String r = context.toString();

    assertThat(r).contains("description=" + description);
  }

  @Test
  public void numRegisteredActions() {
    int numActions = 3;

    for (int numAdded = 0; numAdded < numActions; numAdded++) {
      assertThat(context.getNumRegisteredActions())
          .isEqualTo(numAdded);

      CleanAction a = mock(CleanAction.class);
      context.add(a);
    }

    assertThat(context.getNumRegisteredActions())
        .isEqualTo(numActions);
  }

  @Test
  public void numRegisteredActionsZeroAfterClose() throws CloseFailuresException {
    CleanAction a = mock(CleanAction.class);
    context.add(a);

    context.close();

    assertThat(context.getNumRegisteredActions())
        .isZero();
  }
}
