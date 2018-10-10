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

import com.exonum.binding.common.proofs.map.CheckedMapProof;
import com.exonum.binding.common.proofs.map.MapEntry;
import com.exonum.binding.common.proofs.map.MapProofStatus;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;

class CheckedMapProofMatcher extends TypeSafeMatcher<CheckedMapProof> {

  private static final BaseEncoding HEX_ENCODING = BaseEncoding.base16().lowerCase();

  private final List<MapTestEntry> entries;

  private final Matcher<Set<ByteString>> missingKeysMatcher;
  private final Matcher<Set<MapEntry>> presentEntriesMatcher;

  private CheckedMapProofMatcher(List<MapTestEntry> entries)
  {
    this.entries = entries;
    Set<MapEntry> expectedEntries = entries.stream()
        .filter(e -> e.getValue().isPresent())
        // TODO: refactor
        .map(e ->
            new MapEntry(ByteString.copyFrom(e.getKey().asBytes()),
                ByteString.copyFromUtf8(e.getValue().get())))
        .collect(Collectors.toSet());
    Set<ByteString> expectedMissingKeys = entries.stream()
        .filter(e -> !e.getValue().isPresent())
        // TODO: refactor
        .map(e -> ByteString.copyFrom(e.getKey().asBytes()))
        .collect(Collectors.toSet());
    this.presentEntriesMatcher = IsEqual.equalTo(expectedEntries);
    this.missingKeysMatcher = IsEqual.equalTo(expectedMissingKeys);
  }

  @Override
  protected boolean matchesSafely(CheckedMapProof checkedMapProof) {
    MapProofStatus status = checkedMapProof.getStatus();
    List<MapEntry> presentEntries = checkedMapProof.getEntries();
    List<ByteString> missingKeys = checkedMapProof.getMissingKeys();
    return status == MapProofStatus.CORRECT
        // TODO: refactor (probably return Set from checked proof)
        && presentEntriesMatcher.matches(new HashSet<>(presentEntries))
        // TODO: refactor (probably return Set from checked proof)
        && missingKeysMatcher.matches(new HashSet<>(missingKeys));
  }

  @Override
  public void describeTo(Description description) {
    String entriesString = entries.stream()
        .map(CheckedMapProofMatcher::formatMapMatcherEntry)
        .collect(Collectors.joining(", ", "[", "]"));
    description.appendText("valid proof, entries=").appendText(entriesString);
  }

  @Override
  protected void describeMismatchSafely(CheckedMapProof proof, Description description) {
    description.appendText("was ");
    MapProofStatus proofStatus = proof.getStatus();
    if (proofStatus == MapProofStatus.CORRECT) {
      // We convert entries to string manually here instead of using MapEntry#toString
      // to decode the value from UTF-8 bytes into Java String (which is passed as
      // the expected value).
      String entries = proof.getEntries().stream()
          .map(CheckedMapProofMatcher::formatMapEntry)
          .collect(Collectors.joining(", ", "[", "]"));

      String missingKeys = proof.getMissingKeys().stream()
          // TODO: refactor
          .map(ByteString::toByteArray)
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
    // TODO: refactor
    String key = HEX_ENCODING.encode(e.getKey().toByteArray());
    // TODO: refactor
    String value = StandardSerializers.string().fromBytes(e.getValue().toByteArray());
    return String.format("(%s -> %s)", key, value);
  }

  private static String formatMapMatcherEntry(MapTestEntry e) {
    String key = e.getKey().toString();
    return e.getValue()
        .map(value -> String.format("(%s -> %s)", key, value))
        .orElse(key);
  }

  /**
   * Creates a matcher of a checked proof that is valid, and has a proof for expected entries
   * presence or absence.
   *
   * @param expectedEntries list of expected entries
   */
  static CheckedMapProofMatcher isValid(List<MapTestEntry> expectedEntries) {
    return new CheckedMapProofMatcher(expectedEntries);
  }
}
