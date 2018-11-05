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

import static com.exonum.binding.test.TestParameters.parameters;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.storage.database.Database;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.indices.IndexConstructors.PartiallyAppliedIndexConstructor;
import com.exonum.binding.util.LibraryLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PrefixNameParameterizedIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @ParameterizedTest(name = "{index} -> {0}")
  @MethodSource("testData")
  void testIndexCtor_ThrowsIfInvalidName(String name, PartiallyAppliedIndexConstructor indexFactory)
      throws Exception {
    try (Cleaner cleaner = new Cleaner();
         Database database = MemoryDb.newInstance()) {
      Snapshot view = database.createSnapshot(cleaner);

      assertThrows(Exception.class, () -> indexFactory.create(name, view));
    }
  }

  private static List<Arguments> testData() {
    return merge(
        asList(
            new Object[]{null},
            parameters(""),
            parameters(" name"),
            parameters("name "),
            parameters("name 1"),
            parameters(" name "),
            parameters("?name"),
            parameters("name?"),
            parameters("na?me"),
            parameters("name#1"),
            parameters("name-1")),
        asList(
            parameters(IndexConstructors.fromOneArg(ListIndexProxy::newInstance)),
            parameters(IndexConstructors.fromOneArg(ProofListIndexProxy::newInstance)),
            parameters(IndexConstructors.fromTwoArg(MapIndexProxy::newInstance)),
            parameters(IndexConstructors.fromTwoArg(ProofMapIndexProxy::newInstance)),
            parameters(IndexConstructors.fromOneArg(ValueSetIndexProxy::newInstance)),
            parameters(IndexConstructors.fromOneArg(KeySetIndexProxy::newInstance)),
            parameters(IndexConstructors.fromOneArg(EntryIndexProxy::newInstance))
        ));
  }

  @SuppressWarnings("unchecked")
  private static <T> List<Arguments> merge(Collection<T[]> a, Collection<T[]> b) {
    int size = a.size() * b.size();
    List<Arguments> merged = new ArrayList<>(size);
    for (T[] first : a) {
      for (T[] second : b) {
        T[] r = (T[]) new Object[first.length + second.length];
        System.arraycopy(first, 0, r, 0, first.length);
        System.arraycopy(second, 0, r, first.length, second.length);
        merged.add(Arguments.of(r));
      }
    }
    return merged;
  }
}
