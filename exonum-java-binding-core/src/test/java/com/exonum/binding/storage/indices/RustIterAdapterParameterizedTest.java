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

package com.exonum.binding.storage.indices;

import static com.exonum.binding.test.TestParameters.parameters;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.runners.Parameterized.Parameter;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class RustIterAdapterParameterizedTest {

  @Parameter(0) public List<Integer> underlyingList;

  RustIterAdapter<Integer> iterAdapter;

  @Test
  public void iteratorMustIncludeAllTheItemsFromTheList() {
    // Create an adapter under test, converting a list to rustIter.
    iterAdapter = new RustIterAdapter<>(
        rustIterMockFromIterable(underlyingList));

    // Use an adapter as Iterator to collect all items in a list
    List<Integer> itemsFromIterAdapter = ImmutableList.copyOf(iterAdapter);

    // check that the lists are the same.
    assertThat(itemsFromIterAdapter, equalTo(underlyingList));
  }

  private static RustIter<Integer> rustIterMockFromIterable(Iterable<Integer> iterable) {
    return new RustIterTestFake(iterable);
  }

  @Parameters
  public static Collection<Object[]> testData() {
    return asList(
        parameters(emptyList()),
        parameters(singletonList(1)),
        parameters(asList(1, 2)),
        parameters(asList(1, 2, 3)),
        parameters(asList(1, 2, 3, 4, 5))
    );
  }
}
