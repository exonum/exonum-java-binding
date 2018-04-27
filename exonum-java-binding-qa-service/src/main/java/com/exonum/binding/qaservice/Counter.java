package com.exonum.binding.qaservice;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;

final class Counter {

  private final String name;
  private final long value;

  Counter(String name, long value) {
    this.name = checkNotNull(name);
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Counter counter = (Counter) o;
    return value == counter.value
        && Objects.equal(name, counter.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, value);
  }
}
