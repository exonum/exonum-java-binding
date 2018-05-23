package com.exonum.binding.storage.indices;

import static com.exonum.binding.test.Bytes.bytes;
import static com.exonum.binding.test.TestParameters.parameters;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.serialization.StandardSerializers;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ListIndexProxyGroupParameterizedIntegrationTest extends BaseIndexGroupTestable {

  private static final String GROUP_NAME = "list_proxy_group_IT";

  @Parameter(0)
  public PartiallyAppliedListGroupConstructor<ListIndex<String>> listFactory;

  @Parameter(1)
  public String testName;

  @FunctionalInterface
  interface PartiallyAppliedListGroupConstructor<ListIndexT extends ListIndex> {
    ListIndexT create(String groupName, byte[] listId, View view);
  }

  @Test
  public void listsInGroupMustBeIndependent() {
    View view = db.createFork(cleaner);

    // Values to be put in lists, indexed by a list identifier
    ListMultimap<String, String> elementsById = ArrayListMultimap.create();

    elementsById.putAll("1", asList("V1", "V2", "V3"));
    elementsById.putAll("2", asList("V4", "V5", "V6"));
    elementsById.putAll("3", asList("V1", "V2"));
    elementsById.putAll("4", asList("V2", "V1"));
    elementsById.putAll("5", singleton("V10"));
    elementsById.putAll("6", emptySet());

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

  private ListIndex<String> createInGroup(byte[] id, View view) {
    return listFactory.create(GROUP_NAME, id, view);
  }

  private static <E> List<E> getAllValuesFrom(ListIndex<E> list) {
    return ImmutableList.copyOf(list.iterator());
  }

  @Parameters(name = "{index}: {1}")
  public static Collection<Object[]> testData() {
    PartiallyAppliedListGroupConstructor list = (name, listId, view) ->
        ListIndexProxy.newInGroupUnsafe(name, listId, view, StandardSerializers.string());
    PartiallyAppliedListGroupConstructor proofList = (name, listId, view) ->
        ProofListIndexProxy.newInGroupUnsafe(name, listId, view, StandardSerializers.string());
    return asList(
        parameters(list, "ListIndex"),
        parameters(proofList, "ProofListIndex")
    );
  }
}
