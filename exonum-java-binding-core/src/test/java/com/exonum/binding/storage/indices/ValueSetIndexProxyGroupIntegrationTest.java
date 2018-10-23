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

package com.exonum.binding.storage.indices;

import static com.exonum.binding.test.Bytes.bytes;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.storage.database.View;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.SetMultimap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class ValueSetIndexProxyGroupIntegrationTest extends BaseIndexGroupTestable {

  private static final String GROUP_NAME = "value_set_IT";

  @Test
  public void setsInGroupMustBeIndependent() {
    View view = db.createFork(cleaner);

    // Values to be put in sets, indexed by a set identifier.
    SetMultimap<String, String> valuesById = HashMultimap.create();
    valuesById.putAll("1", asList("V1", "V2", "V3"));
    valuesById.putAll("2", asList("V4", "V5", "V6"));
    valuesById.putAll("3", asList("V1", "V2"));
    valuesById.putAll("4", singleton("V10"));
    valuesById.putAll("5", emptySet());

    // Create a set proxy for each id
    Map<String, ValueSetIndexProxy<String>> setsById = new HashMap<>();
    for (String setId : valuesById.keys()) {
      byte[] id = bytes(setId);
      ValueSetIndexProxy<String> set = createInGroup(id, view);

      setsById.put(setId, set);
    }

    // Put elements in each set in the group
    for (Map.Entry<String, ValueSetIndexProxy<String>> entry : setsById.entrySet()) {
      String id = entry.getKey();
      ValueSetIndexProxy<String> set = entry.getValue();

      Set<String> values = valuesById.get(id);
      values.forEach(set::add);
    }

    // Check that each set contains exactly the elements that were added
    for (Map.Entry<String, ValueSetIndexProxy<String>> entry : setsById.entrySet()) {
      String id = entry.getKey();
      ValueSetIndexProxy<String> set = entry.getValue();

      Set<String> actualValuesInSet = getAllValuesFrom(set);
      Set<String> expectedValues = valuesById.get(id);
      assertThat(actualValuesInSet).isEqualTo(expectedValues);
    }
  }

  private ValueSetIndexProxy<String> createInGroup(byte[] id1, View view) {
    return ValueSetIndexProxy.newInGroupUnsafe(GROUP_NAME, id1, view,
        StandardSerializers.string());
  }

  private static <E> Set<E> getAllValuesFrom(ValueSetIndexProxy<E> set) {
    return ImmutableSet.copyOf(Iterators.transform(set.iterator(),
        ValueSetIndexProxy.Entry::getValue));
  }
}
