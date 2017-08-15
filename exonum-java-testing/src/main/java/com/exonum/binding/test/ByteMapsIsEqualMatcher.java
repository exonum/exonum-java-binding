package com.exonum.binding.test;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ByteMapsIsEqualMatcher<K extends Comparable>
    extends TypeSafeMatcher<Map<? super K, byte[]>> {

  private final Map<? super K, byte[]> expectedMap;

  ByteMapsIsEqualMatcher(Map<? super K, byte[]> expectedMap) {
    this.expectedMap = checkNotNull(expectedMap);
  }

  @Override
  protected boolean matchesSafely(Map<? super K, byte[]> map) {
    if (map.size() != expectedMap.size()) {
      return false;
    }
    Iterator<? extends Map.Entry<? super K, byte[]>> expectedIter
        = expectedMap.entrySet().iterator();
    Iterator<? extends Map.Entry<? super K, byte[]>> actualIter
        = map.entrySet().iterator();

    while (expectedIter.hasNext()) {
      assert actualIter.hasNext();

      Map.Entry<? super K, byte[]> expected = expectedIter.next();
      Map.Entry<? super K, byte[]> actual = actualIter.next();
      if (!expected.getKey().equals(actual.getKey())) {
        return false;
      }
      if (!Arrays.equals(expected.getValue(), actual.getValue())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("map containing ").appendText(byteMapToString(expectedMap));
  }

  @Override
  protected void describeMismatchSafely(Map<? super K, byte[]> map,
                                        Description mismatchDescription) {
    mismatchDescription.appendText("map was ").appendText(byteMapToString(map));
  }

  private String byteMapToString(Map<? super K, byte[]> map) {
    StringBuilder sb = new StringBuilder().append('[');
    for (Map.Entry<? super K, byte[]> entry : map.entrySet()) {
      sb.append('(').append(entry.getKey())
          .append(" -> ").append(Arrays.toString(entry.getValue())).append("), ");
    }
    sb.append(']');
    return sb.toString();
  }

  /**
   * Creates a matcher that matches iff the target map contains exactly the same arguments
   * as the expected one, in the same order.
   *
   * @param expected the expected map
   * @param <K> a key type
   */
  public static <K extends Comparable> ByteMapsIsEqualMatcher<K> equalTo(
      Map<? super K, byte[]> expected) {
    return new ByteMapsIsEqualMatcher<>(expected);
  }
}
