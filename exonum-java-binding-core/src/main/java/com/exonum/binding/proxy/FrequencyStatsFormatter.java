package com.exonum.binding.proxy;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unused")  // todo: Add back to Cleaner.
final class FrequencyStatsFormatter {

  static <T> String itemsByTypeFrequency(Collection<T> items) {
    Map<? extends Class<?>, Long> numItemsByType = items.stream()
        .collect(groupingBy(T::getClass, counting()));

    String itemsByTypeFrequency = numItemsByType.entrySet().stream()
        .sorted(Comparator.<Map.Entry<? extends Class<?>, Long>>
            comparingLong(Map.Entry::getValue).reversed())
        .map(Object::toString)
        .collect(Collectors.joining(", "));

    return itemsByTypeFrequency;
  }

  private FrequencyStatsFormatter() {}
}
