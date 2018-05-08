package com.exonum.binding.proxy;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import org.junit.Test;

public class FrequencyStatsFormatterTest {

  @Test
  public void itemsFrequencyNoItems() {
    Collection<?> c = Collections.emptyList();

    String s = FrequencyStatsFormatter.itemsFrequency(c, Object::getClass);

    assertThat(s).isEqualTo("{}");
  }

  @Test
  public void itemsFrequencyOneItem() {
    Collection<Boolean> c = ImmutableList.of(true);

    String s = FrequencyStatsFormatter.itemsFrequency(c, Boolean::booleanValue);

    assertThat(s).isEqualTo("{true=1}");
  }

  @Test
  public void itemsFrequencySeveralItemsSameCategory() {
    Collection<Boolean> c = ImmutableList.of(true, true);

    String s = FrequencyStatsFormatter.itemsFrequency(c, Boolean::booleanValue);

    assertThat(s).isEqualTo("{true=2}");
  }

  @Test
  public void itemsFrequencyMoreTrue() {
    Collection<Boolean> c = ImmutableList.of(true, true, false);

    String s = FrequencyStatsFormatter.itemsFrequency(c, Boolean::booleanValue);

    assertThat(s).isEqualTo("{true=2, false=1}");
  }

  @Test
  public void itemsFrequencyMoreFalse() {
    Collection<Boolean> c = ImmutableList.of(false, false, true);

    String s = FrequencyStatsFormatter.itemsFrequency(c, Boolean::booleanValue);

    assertThat(s).isEqualTo("{false=2, true=1}");
  }

  @Test
  public void itemsFrequencySeveralElementsSameFrequency() {
    Collection<Boolean> c = ImmutableList.of(false, false, true, true);

    String s = FrequencyStatsFormatter.itemsFrequency(c, Boolean::booleanValue);

    assertThat(s).matches("\\{((true=2, false=2)|(false=2, true=2))\\}");
  }

  @Test
  public void itemsFrequency() {
    Collection<String> c = ImmutableList.of("aa", "bb", "cc", "a", "c", "");

    String s = FrequencyStatsFormatter.itemsFrequency(c, String::length);

    assertThat(s).isEqualTo("{2=3, 1=2, 0=1}");
  }
}
