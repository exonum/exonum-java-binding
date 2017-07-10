package com.exonum.binding.storage;

import com.exonum.binding.proxy.RustIter;
import java.util.Iterator;

/**
 * StorageIterator is both an {@link Iterator} and {@link AutoCloseable}.
 *
 * <p>Such interface is needed until an automatic resource management is implemented for
 * all {@link RustIter}s. Until then, you have to close your iterators when you're done with them.
 *
 * @param <E> type of the entry.
 */
interface StorageIterator<E> extends Iterator<E>, AutoCloseable {}
