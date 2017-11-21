package com.exonum.binding.service;

import com.google.common.hash.HashCode;
import java.util.Collections;
import java.util.List;

/**
 * A schema of the tables (= indices) of a service.
 */
public interface Schema {

  /**
   * Returns the root hashes of Merklized tables in this database schema, as of the current
   * state of the database. If there are no Merklized tables, returns an empty list.
   */
  default List<HashCode> getStateHashes() {
    return Collections.emptyList();
  }
}
