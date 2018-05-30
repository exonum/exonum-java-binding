package com.exonum.binding.service;

import com.exonum.binding.hash.HashCode;
import java.util.List;

/**
 * A schema of the tables (= indices) of a service.
 */
public interface Schema {

  /**
   * Returns the root hashes of Merklized tables in this database schema, as of the current
   * state of the database. If there are no Merklized tables, returns an empty list.
   */
  List<HashCode> getStateHashes();
}
