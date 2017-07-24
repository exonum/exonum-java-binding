package com.exonum.binding.proxy;

import java.util.Iterator;

/**
 * StorageIterator is both an {@link Iterator} and {@link NativeProxy}.
 *
 * <p>Such interface is needed until an automatic resource management is implemented for
 * all {@link RustIter}s. Until then, you have to close your iterators when you're done with them.
 *
 * @param <E> type of the entry.
 */
public interface StorageIterator<E> extends Iterator<E>, NativeProxy {}
