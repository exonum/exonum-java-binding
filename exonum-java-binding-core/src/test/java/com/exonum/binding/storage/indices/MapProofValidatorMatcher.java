package com.exonum.binding.storage.indices;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.storage.proofs.map.MapProofValidator;
import com.google.common.hash.HashCode;
import java.util.Arrays;
import javax.annotation.Nullable;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;

// This class is a slightly modified copy of the one in exonum-java-proofs.
class MapProofValidatorMatcher extends TypeSafeMatcher<MapProofValidator> {

  private final byte[] key;
  @Nullable private final byte[] expectedValue;
  private final Matcher<byte[]> keyMatcher;
  private final Matcher<byte[]> valueMatcher;

  MapProofValidatorMatcher(byte[] key, @Nullable byte[] expectedValue) {
    this.key = checkNotNull(key);
    this.expectedValue = expectedValue;
    keyMatcher = IsEqual.equalTo(key);
    valueMatcher = IsEqual.equalTo(expectedValue);
  }

  @Override
  protected boolean matchesSafely(MapProofValidator validator) {
    return validator.isValid()
        && keyMatcher.matches(validator.getKey())
        && validator.getValue()
            .map(valueMatcher::matches)
            .orElse(expectedValue == null);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("valid proof, key=").appendText(hashToString(key))
        .appendText(", value=").appendText(Arrays.toString(expectedValue));
  }

  /**
   * Converts a hash to string.
   *
   * <p>As users mock {@link com.exonum.binding.hash.Hashes},
   * its #toHexString method might not work. That is a mock-safe alternative.
   */
  static String hashToString(byte[] hash) {
    return HashCode.fromBytes(hash).toString();
  }

  /**
   * Creates a matcher of a proof validator that is valid and has
   * the same key and value as specified.
   *
   * @param key a requested key
   * @param expectedValue a value that is expected to be mapped to the requested key,
   *                      or null if there must not be such mapping in the proof map
   */
  static MapProofValidatorMatcher isValid(byte[] key, @Nullable byte[] expectedValue) {
    return new MapProofValidatorMatcher(key, expectedValue);
  }
}
