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
import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.common.collect.MapEntry;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.storage.database.View;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Ignore;
import org.junit.Test;

public class MapIndexProxyGroupIntegrationTest extends BaseMapIndexGroupTestable<String> {

  private static final String GROUP_NAME = "map_group_IT";

  @Ignore("Fails until Lobster Nice is done, see the requirement #3")
  @Test
  public void mapsInGroupWithPrefixIdsAreIndependent() {
    View view = db.createFork(cleaner);

    // A string that will be sliced into pairs of an id and a user key that result
    // in the same database key. Lengths of index ids are in range [1, N-1],
    // each user key has length 'N - length(id)', where N is the length of the string below.
    //
    // In the current implementation, Exonum uses the id as a prefix of a user key to make
    // a database key: database-key = prefix + user-key.
    // Therefore, seemingly isolated collections will have their entries overwritten through
    // other collections.
    String idKeyPrototype = "a collection id and key";

    // Create a Map<Id, MapEntry<Key, Value>>
    Map<String, MapEntry<String, String>> entryById =
        IntStream.range(1, idKeyPrototype.length() - 1)
            .mapToObj(prefixSize -> idAndKeyValue(idKeyPrototype, prefixSize))
            .collect(Collectors.toMap(MapEntry::getKey, MapEntry::getValue));

    // Create a map for each id
    Map<String, MapIndex<String, String>> mapsById = new HashMap<>();
    for (String mapId : entryById.keySet()) {
      MapIndex<String, String> map = createInGroup(bytes(mapId), view);
      mapsById.put(mapId, map);
    }

    // Add in each map the corresponding entry.
    for (Map.Entry<String, MapEntry<String, String>> entry : entryById.entrySet()) {
      String mapId = entry.getKey();
      MapIndex<String, String> map = mapsById.get(mapId);

      MapEntry<String, String> mapEntry = entry.getValue();
      map.put(mapEntry.getKey(), mapEntry.getValue());
    }

    // Check that each map contains the expected entry.
    for (Map.Entry<String, MapEntry<String, String>> entry : entryById.entrySet()) {
      String mapId = entry.getKey();
      MapIndex<String, String> map = mapsById.get(mapId);

      MapEntry<String, String> mapEntry = entry.getValue();
      String key = mapEntry.getKey();
      String expectedValue = mapEntry.getValue();

      assertThat(map.containsKey(key)).isTrue();
      assertThat(map.get(key))
          .as("id='%s', map=%s", mapId, map)
          .isEqualTo(expectedValue);
    }
  }

  private static MapEntry<String, MapEntry<String, String>> idAndKeyValue(String idKeyPrototype,
                                                                          int prefixSize) {
    // A map id (= prefix in the current implementation)
    String mapId = idKeyPrototype.substring(0, prefixSize);
    String userKey = idKeyPrototype.substring(prefixSize);
    assert (mapId + userKey).equals(idKeyPrototype);
    // Make a value that includes the unique user key.
    String value = "value for key='" + userKey + "'";
    return MapEntry.valueOf(mapId, MapEntry.valueOf(userKey, value));
  }

  @Override
  ImmutableMap<String, ImmutableMap<String, String>> getTestEntriesById() {
    return ImmutableMap.<String, ImmutableMap<String, String>>builder()
            .put("1", ImmutableMap.of())
            .put("2", ImmutableMap.of("K1", "V1"))
            .put("3", ImmutableMap.of("K2", "V2", "K3", "V3"))
            .put("4", ImmutableMap.of("K3", "V3", "K2", "V2"))
            .put("5", ImmutableMap.of("K4", "V5", "K6", "V6", "K7", "V7"))
            .build();
  }

  @Override
  MapIndex<String, String> createInGroup(byte[] mapId, View view) {
    return MapIndexProxy.newInGroupUnsafe(GROUP_NAME, mapId, view,
        StandardSerializers.string(), StandardSerializers.string());
  }
}
