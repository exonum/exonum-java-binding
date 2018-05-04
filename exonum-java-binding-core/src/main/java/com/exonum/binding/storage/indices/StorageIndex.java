package com.exonum.binding.storage.indices;

import com.exonum.binding.proxy.NativeProxy;
import com.exonum.binding.storage.database.View;

/**
 * Storage index is a persistent, named collection built on top of Exonum key-value storage.
 *
 * <p>Also known as a collection, a table, and also as (rarely) a view for
 * a {@linkplain View database view} is inherently associated with an index.
 *
 * @see NativeProxy
 */
// todo: Document the various index types if this becomes public (ECR-595)
interface StorageIndex {

  /** Returns the name of this index. */
  String getName();
}
