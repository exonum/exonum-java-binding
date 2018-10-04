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

import com.exonum.binding.common.proofs.map.flat.CheckedMapProof;
import com.exonum.binding.common.proofs.map.flat.MapEntry;
import com.exonum.binding.common.proofs.map.flat.ProofStatus;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.google.common.io.BaseEncoding;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;

class CheckedMapProofMultiMatcher extends TypeSafeMatcher<CheckedMapProof> {

  private static final BaseEncoding HEX_ENCODING = BaseEncoding.base16().lowerCase();

  private final List<MapTestEntry> entries;

  private CheckedMapProofMultiMatcher(List<MapTestEntry> entries) {
    this.entries = entries;
  }

  @Override
  protected boolean matchesSafely(CheckedMapProof checkedMapProof) {
    ProofStatus status = checkedMapProof.getStatus();
    return status == ProofStatus.CORRECT
        && checkProofSize(checkedMapProof)
        && entries.stream().allMatch(e -> checkEntry(checkedMapProof, e));
  }

  private boolean checkProofSize(CheckedMapProof checkedMapProof) {
    List<MapEntry> presentEntries = checkedMapProof.getEntries();
    List<byte[]> missingKeys = checkedMapProof.getMissingKeys();
    long expectedPresentEntries = entries
        .stream()
        .filter(e -> e.getValue().isPresent())
        .count();
    long expectedAbsentEntries = entries
        .stream()
        .filter(e -> !e.getValue().isPresent())
        .count();
    return presentEntries.size() == expectedPresentEntries
        && missingKeys.size() == expectedAbsentEntries;
  }

  private boolean checkEntry(CheckedMapProof checkedMapProof, MapTestEntry entry) {
    Optional<String> entryValue = entry.getValue();
    Matcher<byte[]> keyMatcher = IsEqual.equalTo(entry.getKey().asBytes());

    if (entryValue.isPresent()) {
      Matcher<byte[]> valueMatcher = IsEqual.equalTo(entryValue.get().getBytes());
      List<MapEntry> presentEntries = checkedMapProof.getEntries();
      return checkPresentEntry(presentEntries, keyMatcher, valueMatcher);
    } else {
      List<byte[]> missingKeys = checkedMapProof.getMissingKeys();
      return checkAbsentEntry(missingKeys, keyMatcher);
    }
  }

  private boolean checkPresentEntry(
      List<MapEntry> presentEntries, Matcher<byte[]> keyMatcher, Matcher<byte[]> valueMatcher) {
    return presentEntries
        .stream()
        .anyMatch(presentEntry -> keyMatcher.matches(presentEntry.getKey())
            && valueMatcher.matches(presentEntry.getValue()));
  }

  private boolean checkAbsentEntry(List<byte[]> missingKeys, Matcher<byte[]> keyMatcher) {
    return missingKeys
        .stream()
        .anyMatch(keyMatcher::matches);
  }

  @Override
  public void describeTo(Description description) {
    String entriesString = entries.stream()
        .map(CheckedMapProofMultiMatcher::formatMapMatcherEntry)
        .collect(Collectors.joining(", ", "[", "]"));
    description.appendText("valid proof, entries=").appendText(entriesString);
  }

  @Override
  protected void describeMismatchSafely(CheckedMapProof proof, Description description) {
    description.appendText("was ");
    ProofStatus proofStatus = proof.getStatus();
    if (proofStatus == ProofStatus.CORRECT) {
      // We convert entries to string manually here instead of using MapEntry#toString
      // to decode the value from UTF-8 bytes into Java String (which is passed as
      // the expected value).
      String entries = proof.getEntries().stream()
          .map(CheckedMapProofMultiMatcher::formatMapEntry)
          .collect(Collectors.joining(", ", "[", "]"));

      String missingKeys = proof.getMissingKeys().stream()
          .map(HEX_ENCODING::encode)
          .collect(Collectors.joining(", ", "[", "]"));

      description.appendText("a valid proof, entries=").appendText(entries)
          .appendText(", missing keys=").appendText(missingKeys)
          .appendText(", Merkle root=").appendValue(proof.getRootHash());
    } else {
      description.appendText("an invalid proof, status=")
          .appendValue(proofStatus);
    }
  }

  private static String formatMapEntry(MapEntry e) {
    String key = HEX_ENCODING.encode(e.getKey());
    String value = StandardSerializers.string().fromBytes(e.getValue());
    return String.format("(%s -> %s)", key, value);
  }

  private static String formatMapMatcherEntry(MapTestEntry e) {
    String key = e.getKey().toString();
    return e.getValue().isPresent()
        ? String.format("(%s -> %s)", key, e.getValue().get())
        : key;
  }

  /**
   * Creates a matcher of a checked proof that is valid, and has a proof for expected entries
   * presence or absence.
   *
   * @param expectedEntries list of expected entries
   */
  static CheckedMapProofMultiMatcher isValid(List<MapTestEntry> expectedEntries) {
    return new CheckedMapProofMultiMatcher(expectedEntries);
  }
}
