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

import static com.exonum.binding.storage.indices.MapTestEntry.presentEntry;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.asList;
import static java.util.stream.Collectors.toList;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.proofs.map.CheckedMapProof;
import com.exonum.binding.common.proofs.map.UncheckedMapProof;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

class ProofMapContainsMatcher extends TypeSafeMatcher<ProofMapIndexProxy<HashCode, String>> {


  private final List<MapTestEntry> entries;

  private final CheckedMapProofMatcher mapProofMatcher;

  private ProofMapContainsMatcher(List<MapTestEntry> entries) {
    this.entries = entries;
    mapProofMatcher = CheckedMapProofMatcher.isValid(this.entries);
  }

  @Override
  protected boolean matchesSafely(ProofMapIndexProxy<HashCode, String> map) {
    CheckedMapProof checkedProof = checkProof(map);
    HashCode expectedRootHash = map.getRootHash();

    return mapProofMatcher.matches(checkedProof)
        && checkedProof.compareWithRootHash(expectedRootHash);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("proof map providing ")
        .appendDescriptionOf(mapProofMatcher);
  }

  @Override
  protected void describeMismatchSafely(ProofMapIndexProxy<HashCode, String> map,
      Description mismatchDescription) {
    mismatchDescription.appendText("was a proof map with Merkle root=")
        .appendValue(map.getRootHash())
        .appendText(" providing a proof that ");
    CheckedMapProof checkedProof = checkProof(map);
    mapProofMatcher.describeMismatch(checkedProof, mismatchDescription);
  }

  private CheckedMapProof checkProof(ProofMapIndexProxy<HashCode, String> map) {
    Collection<HashCode> keys = entries.stream()
        .map(MapTestEntry::getKey)
        .collect(toList());

    UncheckedMapProof proof = map.getProof(keys);
    assert proof != null : "The proof must not be null";

    return proof.check();
  }

  /**
   * Creates a matcher for a proof map that matches iff the map provides a valid proof that
   * present entries are in the map and absent keys are missing from the map.
   * @param entry expected present or absent map entry
   * @param entries other expected present or absent map entries
   */
  static ProofMapContainsMatcher provesThatCorrect(
      MapTestEntry entry, MapTestEntry... entries) {
    List<MapTestEntry> expectedEntries = asList(entry, entries);
    return new ProofMapContainsMatcher(expectedEntries);
  }

  /**
   * Creates a matcher for a proof map that matches iff the map provides a valid proof that all
   * the entries that are expected to be present are contained in the map.
   *
   * @param expectedPresentEntries expected collection of present map entries
   */
  static ProofMapContainsMatcher provesThatPresent(
      Collection<MapEntry<HashCode, String>> expectedPresentEntries) {
    checkArgument(
        !expectedPresentEntries.isEmpty(), "Expected entries collection shouldn't be empty");
    List<MapTestEntry> expectedEntries = expectedPresentEntries.stream()
        .map(e -> presentEntry(e.getKey(), e.getValue()))
        .collect(toList());
    return new ProofMapContainsMatcher(expectedEntries);
  }

  /**
   * Creates a matcher for a proof map that matches iff the map provides a valid proof that the
   * entry that is expected to be present is contained in the map.
   *
   * @param expectedKey key of expected present map entry
   * @param expectedValue value of expected present map entry
   */
  static ProofMapContainsMatcher provesThatPresent(HashCode expectedKey, String expectedValue) {
    List<MapTestEntry> expectedEntry =
        Collections.singletonList(presentEntry(expectedKey, expectedValue));
    return new ProofMapContainsMatcher(expectedEntry);
  }

  /**
   * Creates a matcher for a proof map that matches iff the map provides a valid proof that all
   * the entries that are expected to be absent are not contained in the map.
   *
   * @param expectedAbsentEntries expected collection of absent map entries
   */
  static ProofMapContainsMatcher provesThatAbsent(Collection<HashCode> expectedAbsentEntries) {
    checkArgument(
        !expectedAbsentEntries.isEmpty(), "Expected entries collection shouldn't be empty");
    List<MapTestEntry> expectedEntries = expectedAbsentEntries.stream()
        .map(MapTestEntry::absentEntry)
        .collect(toList());
    return new ProofMapContainsMatcher(expectedEntries);
  }

  /**
   * Creates a matcher for a proof map that matches iff the map provides a valid proof that all the
   * entries that are expected to be absent are not contained in the map.
   *
   * @param absentEntry expected absent map entry
   * @param absentEntries other expected absent map entries
   */
  static ProofMapContainsMatcher provesThatAbsent(HashCode absentEntry, HashCode... absentEntries) {
    List<HashCode> entries = asList(absentEntry, absentEntries);
    List<MapTestEntry> expectedEntries = entries.stream()
        .map(MapTestEntry::absentEntry)
        .collect(toList());
    return new ProofMapContainsMatcher(expectedEntries);
  }
}
