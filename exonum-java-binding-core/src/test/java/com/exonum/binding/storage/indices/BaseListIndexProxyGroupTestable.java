package com.exonum.binding.storage.indices;

import static com.exonum.binding.test.Bytes.bytes;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.storage.database.View;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

abstract class BaseListIndexProxyGroupTestable extends BaseIndexGroupTestable {

  @Test
  public void listsInGroupMustBeIndependent() {
    View view = db.createFork(cleaner);

    // Values to be put in lists, indexed by a list identifier
    ListMultimap<String, String> elementsById = getTestElementsById();

    // Create a list proxy for each id
    Map<String, ListIndex<String>> listsById = new HashMap<>();
    for (String setId : elementsById.keys()) {
      byte[] id = bytes(setId);
      ListIndex<String> set = createInGroup(id, view);

      listsById.put(setId, set);
    }

    // Add elements to each list in the group
    for (Map.Entry<String, ListIndex<String>> entry : listsById.entrySet()) {
      String id = entry.getKey();
      ListIndex<String> list = entry.getValue();

      List<String> values = elementsById.get(id);
      list.addAll(values);
    }

    // Check that each list contains exactly the elements that were added
    for (Map.Entry<String, ListIndex<String>> entry : listsById.entrySet()) {
      String id = entry.getKey();
      ListIndex<String> list = entry.getValue();

      List<String> actualElementsInList = getAllValuesFrom(list);
      List<String> expectedElements = elementsById.get(id);
      assertThat(actualElementsInList).isEqualTo(expectedElements);
    }
  }

  private ListMultimap<String, String> getTestElementsById() {
    ListMultimap<String, String> elementsById = ArrayListMultimap.create();
    elementsById.putAll("1", asList("V1", "V2", "V3"));
    elementsById.putAll("2", asList("V4", "V5", "V6"));
    elementsById.putAll("3", asList("V1", "V2"));
    elementsById.putAll("4", asList("V2", "V1"));
    elementsById.putAll("5", singleton("V10"));
    elementsById.putAll("6", emptySet());
    return elementsById;
  }

  /** Creates a list-under-test in some group with the given id. */
  abstract ListIndex<String> createInGroup(byte[] id, View view);

  private static <E> List<E> getAllValuesFrom(ListIndex<E> list) {
    return ImmutableList.copyOf(list.iterator());
  }
}
