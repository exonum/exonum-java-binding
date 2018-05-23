package com.exonum.binding.storage.indices;

import static com.exonum.binding.test.Bytes.bytes;
import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.serialization.StandardSerializers;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Ignore;
import org.junit.Test;

public class MapIndexProxyGroupIntegrationTest extends BaseIndexGroupTestable {

  private static final String GROUP_NAME = "map_group_IT";

  @Test
  public void newInGroupUnsafe() {
    View view = db.createFork(cleaner);

    ImmutableMap<String, ImmutableMap<String, String>> entriesById =
        ImmutableMap.<String, ImmutableMap<String, String>>builder()
            .put("1", ImmutableMap.of())
            .put("2", ImmutableMap.of("K1", "V1"))
            .put("3", ImmutableMap.of("K2", "V2", "K3", "V3"))
            .put("4", ImmutableMap.of("K3", "V3", "K2", "V2"))
            .put("5", ImmutableMap.of("K4", "V5", "K6", "V6", "K7", "V7"))
            .build();

    // Create a map for each id
    Map<String, MapIndex<String, String>> mapsById = new HashMap<>();
    for (String mapId : entriesById.keySet()) {
      byte[] id = bytes(mapId);
      MapIndex<String, String> map = createInGroup(id, view);

      mapsById.put(mapId, map);
    }

    // Put entries in each map
    for (Map.Entry<String, MapIndex<String, String>> entry : mapsById.entrySet()) {
      String id = entry.getKey();
      MapIndex<String, String> map = entry.getValue();

      map.putAll(entriesById.get(id));
    }

    // Check that each map contains the added entries
    for (Map.Entry<String, MapIndex<String, String>> entry : mapsById.entrySet()) {
      String id = entry.getKey();
      MapIndex<String, String> map = entry.getValue();

      Map<String, String> actualEntriesInMap = MapEntries.extractEntries(map);
      Map<String, String> expectedEntries = entriesById.get(id);
      assertThat(actualEntriesInMap).isEqualTo(expectedEntries);
    }
  }

  @Ignore("Fails until Lobster Nice is done, see the requirement #3")
  @Test
  public void mapsInGroupWithPrefixIdsAreIndependent() {
    View view = db.createFork(cleaner);

    // A string that will be sliced in an id of length [1, N-1] and user key of length [N-1, 1].
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

  private static MapIndex<String, String> createInGroup(byte[] mapId, View view) {
    return MapIndexProxy.newInGroupUnsafe(GROUP_NAME, mapId, view,
        StandardSerializers.string(), StandardSerializers.string());
  }

  private static MapEntry<String, MapEntry<String, String>> idAndKeyValue(String idKeyPrototype,
                                                                          int prefixSize) {
    // A map id (= prefix in the current implementation)
    String mapId = idKeyPrototype.substring(0, prefixSize);
    String userKey = idKeyPrototype.substring(prefixSize, idKeyPrototype.length());
    assert (mapId + userKey).equals(idKeyPrototype);
    // Make a value that includes the unique user key.
    String value = "value for key='" + userKey + "'";
    return MapEntry.from(mapId, MapEntry.from(userKey, value));
  }
}
