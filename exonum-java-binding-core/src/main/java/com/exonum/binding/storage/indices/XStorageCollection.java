package com.exonum.binding.storage.indices;

import com.exonum.binding.storage.database.ViewProxy;

/**
 * An Exonum storage collection is a persistent, named collection built on top of Exonum
 * key-value storage.
 *
 * <p>Also known as an index, a table, and also as (rarely) a view for
 * a {@linkplain ViewProxy database view} is inherently associated with an index.
 */
public interface XStorageCollection {

  /** Returns the name of this index. */
  String getName();
}
