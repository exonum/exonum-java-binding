/* 
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
