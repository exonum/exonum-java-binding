package com.exonum.binding.proxy;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import org.junit.Test;

public class FrequencyStatsFormatterTest {

  @Test
  public void itemsByTypeFrequencyNoItems() {
    Collection<?> c = Collections.emptyList();

    String s = FrequencyStatsFormatter.itemsByTypeFrequency(c);

    assertThat(s).isEmpty();
  }

  @Test
  public void itemsByTypeFrequencyOneItem() {
    Collection<?> c = ImmutableList.of(new Object());

    String s = FrequencyStatsFormatter.itemsByTypeFrequency(c);

    assertThat(s).isEqualTo("class java.lang.Object=1");
  }

  @Test
  public void itemsByTypeFrequencySeveralItemsSameType() {
    Collection<?> c = ImmutableList.of(new Object(), new Object());

    String s = FrequencyStatsFormatter.itemsByTypeFrequency(c);

    assertThat(s).isEqualTo("class java.lang.Object=2");
  }

  @Test
  public void itemsByTypeFrequencyMoreObjects() {
    Collection<?> c = ImmutableList.of(new Object(), new Object(), "");

    String s = FrequencyStatsFormatter.itemsByTypeFrequency(c);

    assertThat(s).isEqualTo("class java.lang.Object=2, class java.lang.String=1");
  }

  @Test
  public void itemsByTypeFrequencyMoreStrings() {
    Collection<?> c = ImmutableList.of(new Object(), "s1", "s2");

    String s = FrequencyStatsFormatter.itemsByTypeFrequency(c);

    assertThat(s).isEqualTo("class java.lang.String=2, class java.lang.Object=1");
  }

  @Test
  public void itemsByTypeFrequencySeveralTypesSameFrequency() {
    Collection<?> c = ImmutableList.of(new Object(), new Object(), "s1", "s2");

    String s = FrequencyStatsFormatter.itemsByTypeFrequency(c);

    assertThat(s).matches("(class java.lang.Object=2, class java.lang.String=2)"
        + "|(class java.lang.String=2, class java.lang.Object=2)");
  }
}
