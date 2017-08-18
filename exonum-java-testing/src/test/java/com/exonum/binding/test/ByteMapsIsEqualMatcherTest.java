package com.exonum.binding.test;

import static com.exonum.binding.test.Bytes.bytes;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSortedMap;
import java.util.Map;
import org.junit.Test;

public class ByteMapsIsEqualMatcherTest {

  @Test
  public void matchesSafely_SingletonMap() throws Exception {
    Map<Long, byte[]> expected = ImmutableSortedMap.of(0L, bytes("v1"));
    Map<Long, byte[]> actual = ImmutableSortedMap.of(0L, bytes("v1"));

    ByteMapsIsEqualMatcher<Long> matcher = ByteMapsIsEqualMatcher.equalTo(expected);

    assertTrue(matcher.matchesSafely(actual));
  }

  @Test
  public void matchesSafely_TwoItemMap() throws Exception {
    Map<Long, byte[]> expected = ImmutableSortedMap.of(0L, bytes("v1"),
        1L, bytes("v2"));
    Map<Long, byte[]> actual = ImmutableSortedMap.of(0L, bytes("v1"),
        1L, bytes("v2"));

    ByteMapsIsEqualMatcher<Long> matcher = ByteMapsIsEqualMatcher.equalTo(expected);

    assertTrue(matcher.matchesSafely(actual));
  }

  @Test
  public void matchesSafely_DiffSize() throws Exception {
    Map<Long, byte[]> expected = ImmutableSortedMap.of(0L, bytes("v1"),
        1L, bytes("v2"));

    Map<Long, byte[]> actual = ImmutableSortedMap.of(0L, bytes("v1"));

    ByteMapsIsEqualMatcher<Long> matcher = ByteMapsIsEqualMatcher.equalTo(expected);

    assertFalse(matcher.matchesSafely(actual));
  }

  @Test
  public void matchesSafely_DiffKeys() throws Exception {
    Map<Long, byte[]> expected = ImmutableSortedMap.of(0L, bytes("v1"));

    Map<Long, byte[]> actual = ImmutableSortedMap.of(1234L, bytes("v1"));

    ByteMapsIsEqualMatcher<Long> matcher = ByteMapsIsEqualMatcher.equalTo(expected);

    assertFalse(matcher.matchesSafely(actual));
  }

  @Test
  public void matchesSafely_DiffValues() throws Exception {
    Map<Long, byte[]> expected = ImmutableSortedMap.of(0L, bytes("v1"));
    Map<Long, byte[]> actual = ImmutableSortedMap.of(0L, bytes("otherValue"));

    ByteMapsIsEqualMatcher<Long> matcher = ByteMapsIsEqualMatcher.equalTo(expected);

    assertFalse(matcher.matchesSafely(actual));
  }

}
