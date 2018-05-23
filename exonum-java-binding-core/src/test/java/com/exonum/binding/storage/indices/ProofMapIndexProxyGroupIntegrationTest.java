package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.ProofMapIndexProxyIntegrationTest.PK1;
import static com.exonum.binding.storage.indices.ProofMapIndexProxyIntegrationTest.PK2;
import static com.exonum.binding.storage.indices.ProofMapIndexProxyIntegrationTest.PK3;
import static com.exonum.binding.test.Bytes.bytes;
import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.serialization.StandardSerializers;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class ProofMapIndexProxyGroupIntegrationTest extends BaseIndexGroupTestable {

  private static final String GROUP_NAME = "proof_map_group_IT";

  @Test
  public void newInGroupUnsafe() {
    View view = db.createFork(cleaner);

    ImmutableMap<String, ImmutableMap<HashCode, String>> entriesById =
        ImmutableMap.<String, ImmutableMap<HashCode, String>>builder()
            .put("1", ImmutableMap.of())
            .put("2", ImmutableMap.of(PK1, "V1"))
            .put("3", ImmutableMap.of(PK2, "V2", PK3, "V3"))
            .put("4", ImmutableMap.of(PK3, "V3", PK2, "V2"))
            .put("5", ImmutableMap.of(PK1, "V5", PK2, "V6", PK3, "V7"))
            .build();

    // Create a map for each id
    Map<String, MapIndex<HashCode, String>> mapsById = new HashMap<>();
    for (String mapId : entriesById.keySet()) {
      byte[] id = bytes(mapId);
      MapIndex<HashCode, String> map = createInGroup(id, view);

      mapsById.put(mapId, map);
    }

    // Put entries in each map
    for (Map.Entry<String, MapIndex<HashCode, String>> entry : mapsById.entrySet()) {
      String id = entry.getKey();
      MapIndex<HashCode, String> map = entry.getValue();

      map.putAll(entriesById.get(id));
    }

    // Check that each map contains the added entries
    for (Map.Entry<String, MapIndex<HashCode, String>> entry : mapsById.entrySet()) {
      String id = entry.getKey();
      MapIndex<HashCode, String> map = entry.getValue();

      Map<HashCode, String> actualEntriesInMap = MapEntries.extractEntries(map);
      Map<HashCode, String> expectedEntries = entriesById.get(id);
      assertThat(actualEntriesInMap).isEqualTo(expectedEntries);
    }
  }

  private static ProofMapIndexProxy<HashCode, String> createInGroup(byte[] mapId, View view) {
    return ProofMapIndexProxy.newInGroupUnsafe(GROUP_NAME, mapId, view,
        StandardSerializers.hash(), StandardSerializers.string());
  }
}
