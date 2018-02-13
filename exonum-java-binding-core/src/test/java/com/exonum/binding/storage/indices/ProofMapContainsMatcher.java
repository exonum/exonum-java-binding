package com.exonum.binding.storage.indices;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.storage.proofs.map.MapProof;
import com.exonum.binding.storage.proofs.map.MapProofValidator;
import com.exonum.binding.storage.serialization.StandardSerializers;
import javax.annotation.Nullable;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

class ProofMapContainsMatcher extends TypeSafeMatcher<ProofMapIndexProxy<HashCode, String>> {

  private final HashCode key;
  private final MapProofValidatorMatcher proofValidatorMatcher;

  private ProofMapContainsMatcher(HashCode key, @Nullable String expectedValue) {
    this.key = key;
    proofValidatorMatcher = MapProofValidatorMatcher.isValid(key, expectedValue);
  }

  @Override
  protected boolean matchesSafely(ProofMapIndexProxy<HashCode, String> map) {
    MapProofValidator validator = checkProof(map);

    return proofValidatorMatcher.matches(validator);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("proof map providing ")
        .appendDescriptionOf(proofValidatorMatcher);
  }

  @Override
  protected void describeMismatchSafely(ProofMapIndexProxy<HashCode, String> map,
                                        Description mismatchDescription) {
    MapProofValidator validator = checkProof(map);
    proofValidatorMatcher.describeMismatch(validator, mismatchDescription);
  }

  private MapProofValidator checkProof(ProofMapIndexProxy<HashCode, String> map) {
    MapProof proof = map.getProof(key);
    assert proof != null : "The proof must not be null";
    HashCode rootHash = map.getRootHash();
    MapProofValidator<String> validator = new MapProofValidator<>(rootHash, key.asBytes(),
        StandardSerializers.string());

    proof.accept(validator);
    return validator;
  }

  /**
   * Creates a matcher for a proof map that matches iff the map provides a valid proof
   * that it maps the specified value to the specified key.
   *
   * @param key a key to request proof for
   * @param value an expected value mapped to the key
   */
  static ProofMapContainsMatcher provesThatContains(HashCode key, String value) {
    return new ProofMapContainsMatcher(key, checkNotNull(value));
  }

  /**
   * Creates a matcher for a proof map that matches iff the map provides a valid proof
   * that it does not map any value to the specified key.
   *
   * @param key a key to request proof for
   */
  static ProofMapContainsMatcher provesNoMappingFor(HashCode key) {
    return new ProofMapContainsMatcher(key, null);
  }
}
