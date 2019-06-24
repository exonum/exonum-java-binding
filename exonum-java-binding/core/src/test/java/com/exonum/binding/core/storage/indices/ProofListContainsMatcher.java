/*
 * Copyright 2019 The Exonum Team
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

package com.exonum.binding.core.storage.indices;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.core.IsEqual.equalTo;

import com.exonum.binding.common.proofs.list.CheckedListProof;
import com.exonum.binding.common.proofs.list.UncheckedListProof;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

class ProofListContainsMatcher extends TypeSafeMatcher<ProofListIndexProxy<String>> {

  private final Function<ProofListIndexProxy<String>, UncheckedListProof> proofFunction;
  private final Matcher<Map<Long, String>> elementsMatcher;

  private ProofListContainsMatcher(
      Function<ProofListIndexProxy<String>, UncheckedListProof> proofFunction,
      Map<Long, String> expectedProofElements) {
    this.proofFunction = proofFunction;
    this.elementsMatcher = equalTo(expectedProofElements);
  }

  @Override
  protected boolean matchesSafely(ProofListIndexProxy<String> list) {
    if (list == null) {
      return false;
    }

    UncheckedListProof proof = proofFunction.apply(list);
    CheckedListProof checkedProof = proof.check();

    return checkedProof.isValid()
        && elementsMatcher.matches(checkedProof.getElements())
        && list.getRootHash().equals(checkedProof.getRootHash());
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("proof list containing elements: ")
        .appendDescriptionOf(elementsMatcher);
  }

  @Override
  protected void describeMismatchSafely(ProofListIndexProxy<String> list,
                                        Description mismatchDescription) {
    UncheckedListProof proof = proofFunction.apply(list);
    CheckedListProof checkedProof = proof.check();

    if (!checkedProof.isValid()) {
      mismatchDescription.appendText("proof was not valid: ")
          .appendValue(checkedProof.getProofStatus().getDescription());
      return;
    }

    if (!list.getRootHash().equals(checkedProof.getRootHash())) {
      mismatchDescription.appendText("calculated root hash doesn't match: ")
          .appendValue(checkedProof.getRootHash())
          .appendText("expected root hash: ")
          .appendValue(list.getRootHash());
      return;
    }

    if (!elementsMatcher.matches(checkedProof.getElements())) {
      mismatchDescription.appendText("valid proof: ").appendValue(checkedProof)
          .appendText(", elements mismatch: ");
      elementsMatcher.describeMismatch(checkedProof.getElements(), mismatchDescription);
    }
  }

  /**
   * Creates a matcher for a proof list that will match iff the list contains the specified value
   * at the specified position and provides a <em>valid</em> cryptographic proof of that.
   *
   * <p>The proof is obtained via {@link ProofListIndexProxy#getProof(long)}.
   *
   * @param index an index of the element
   * @param expectedValue an expected value of the element at the given index
   */
  public static ProofListContainsMatcher provesThatContains(long index, String expectedValue) {
    checkArgument(0 <= index);
    checkNotNull(expectedValue);

    Function<ProofListIndexProxy<String>, UncheckedListProof> proofFunction =
        (list) -> list.getProof(index);

    return new ProofListContainsMatcher(proofFunction,
        Collections.singletonMap(index, expectedValue));
  }

  /**
   * Creates a matcher for a proof list that will match iff the list contains the specified values
   * starting at the specified position and provides a <em>valid</em> cryptographic proof of that.
   *
   * <p>The proof is obtained via {@link ProofListIndexProxy#getRangeProof(long, long)}.
   * The value of {@code to} parameter is inferred from the size of the list of expected values.
   *
   * @param from an index of the first element
   * @param expectedValues a list of elements, that are expected to be in the list
   *                       starting at the given index
   * @throws IllegalArgumentException if from is negative or the list is empty
   */
  public static ProofListContainsMatcher provesThatContains(long from,
                                                            List<String> expectedValues) {
    checkArgument(0 <= from, "Range start index (%s) is negative", from);
    checkArgument(!expectedValues.isEmpty(), "Empty list of expected values");

    long to = from + expectedValues.size();
    Function<ProofListIndexProxy<String>, UncheckedListProof> proofFunction =
        (list) -> list.getRangeProof(from, to);

    Map<Long, String> expectedProofElements = new TreeMap<>();
    for (long i = from; i < to; i++) {
      expectedProofElements.put(i, expectedValues.get(Math.toIntExact(i - from)));
    }
    return new ProofListContainsMatcher(proofFunction, expectedProofElements);
  }
}
