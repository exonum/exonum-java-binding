package com.exonum.binding.storage.indices;

import com.exonum.binding.proxy.NativeProxy;
import com.exonum.binding.storage.database.ViewProxy;

/**
 * Storage index is a persistent, named collection built on top of Exonum key-value storage.
 *
 * <p>Also known as a collection, a table, and also as (rarely) a view for
 * a {@linkplain ViewProxy database view} is inherently associated with an index.
 *
 * @implNote currently extends NativeProxy, if we decide to split proxies and user-facing code
 *     (see ECR-595), it must be removed from this interface
 *
 * @see NativeProxy
 */
// todo: Document the various index types if this becomes public (ECR-595)
interface StorageIndex extends NativeProxy {
  /** Returns the name of this index. */
  String getName();
}
