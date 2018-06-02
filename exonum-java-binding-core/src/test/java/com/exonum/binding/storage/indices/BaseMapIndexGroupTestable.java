package com.exonum.binding.storage.indices;

import static com.exonum.binding.test.Bytes.bytes;
import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.storage.database.View;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

abstract class BaseMapIndexGroupTestable<KeyT> extends BaseIndexGroupTestable {

  @Test
  public void newInGroupUnsafe() {
    View view = db.createFork(cleaner);

    ImmutableMap<String, ImmutableMap<KeyT, String>> entriesById = getTestEntriesById();

    // Create a map for each id
    Map<String, MapIndex<KeyT, String>> mapsById = new HashMap<>();
    for (String mapId : entriesById.keySet()) {
      byte[] id = bytes(mapId);
      MapIndex<KeyT, String> map = createInGroup(id, view);

      mapsById.put(mapId, map);
    }

    // Put entries in each map
    for (Map.Entry<String, MapIndex<KeyT, String>> entry : mapsById.entrySet()) {
      String id = entry.getKey();
      MapIndex<KeyT, String> map = entry.getValue();

      map.putAll(entriesById.get(id));
    }

    // Check that each map contains the added entries
    for (Map.Entry<String, MapIndex<KeyT, String>> entry : mapsById.entrySet()) {
      String id = entry.getKey();
      MapIndex<KeyT, String> map = entry.getValue();

      Map<KeyT, String> actualEntriesInMap = MapEntries.extractEntries(map);
      Map<KeyT, String> expectedEntries = entriesById.get(id);
      assertThat(actualEntriesInMap).isEqualTo(expectedEntries);
    }
  }

  /** Creates test entries to be put in maps indexed by their group identifier. */
  abstract ImmutableMap<String, ImmutableMap<KeyT, String>> getTestEntriesById();

  /** Creates a map-under-test in some group with the given id. */
  abstract MapIndex<KeyT, String> createInGroup(byte[] id, View view);

}
