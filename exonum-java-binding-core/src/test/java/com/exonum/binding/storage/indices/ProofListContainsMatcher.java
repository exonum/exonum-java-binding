package com.exonum.binding.storage.indices;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.storage.proofs.list.ListProof;
import com.exonum.binding.storage.proofs.list.ListProofValidator;
import com.exonum.binding.test.ByteMapsIsEqualMatcher;
import com.google.common.hash.HashCode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

class ProofListContainsMatcher extends TypeSafeMatcher<ProofListIndexProxy> {

  private final Function<ProofListIndexProxy, ListProof> proofFunction;
  private final ByteMapsIsEqualMatcher<Long> elementsMatcher;

  ProofListContainsMatcher(Function<ProofListIndexProxy, ListProof> proofFunction,
                           Map<Long, byte[]> expectedProofElements) {
    this.proofFunction = proofFunction;
    this.elementsMatcher = ByteMapsIsEqualMatcher.equalTo(expectedProofElements);
  }

  @Override
  protected boolean matchesSafely(ProofListIndexProxy list) {
    if (list == null) {
      return false;
    }

    ListProof proof = proofFunction.apply(list);
    ListProofValidator validator = new ListProofValidator(HashCode.fromBytes(list.getRootHash()), list.size());  // fixme:
    proof.accept(validator);

    return validator.isValid() && elementsMatcher.matches(validator.getElements());
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("proof list containing elements: ")
        .appendDescriptionOf(elementsMatcher);
  }

  @Override
  protected void describeMismatchSafely(ProofListIndexProxy list, Description mismatchDescription) {
    ListProof proof = proofFunction.apply(list);
    ListProofValidator validator = new ListProofValidator(HashCode.fromBytes(list.getRootHash()), list.size());  // fixme:
    proof.accept(validator);

    if (!validator.isValid()) {
      mismatchDescription.appendText("proof was not valid: ").appendValue(validator);
      return;
    }

    if (!elementsMatcher.matches(validator.getElements())) {
      mismatchDescription.appendText("valid proof: ").appendValue(validator)
          .appendText(", elements mismatch: ");
      elementsMatcher.describeMismatch(validator.getElements(), mismatchDescription);
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
  public static ProofListContainsMatcher provesThatContains(long index, byte[] expectedValue) {
    checkArgument(0 <= index);
    checkNotNull(expectedValue);

    Function<ProofListIndexProxy, ListProof> proofFunction = (list) -> list.getProof(index);

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
                                                            List<byte[]> expectedValues) {
    checkArgument(0 <= from, "Range start index (%s) is negative", from);
    checkArgument(!expectedValues.isEmpty(), "Empty list of expected values");

    long to = from + expectedValues.size();
    Function<ProofListIndexProxy, ListProof> proofFunction = (list) -> list.getRangeProof(from, to);

    Map<Long, byte[]> expectedProofElements = new TreeMap<>();
    for (long i = from; i < to; i++) {
      expectedProofElements.put(i, expectedValues.get(Math.toIntExact(i - from)));
    }
    return new ProofListContainsMatcher(proofFunction, expectedProofElements);
  }
}
