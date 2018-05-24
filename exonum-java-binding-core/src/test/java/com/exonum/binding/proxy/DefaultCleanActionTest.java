package com.exonum.binding.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;

public class DefaultCleanActionTest {

  @Test
  public void resourceTypeEmptyByDefault() {
    // Cast lambda to CleanAction.
    CleanAction<?> a = () -> { };

    // Check the resource type.
    assertThat(a.resourceType()).isEmpty();
  }

  @Test
  public void from() {
    Runnable r = mock(Runnable.class);
    String expectedResourceType = "Native proxy";

    CleanAction<String> a = CleanAction.from(r, expectedResourceType);

    // Check the resource type.
    assertThat(a.resourceType()).hasValue(expectedResourceType);

    // Execute the action.
    a.clean();

    // Verify it executed the runnable.
    verify(r).run();
  }
}
