package com.exonum.binding.storage.indices;

import com.exonum.binding.proxy.NativeProxy;
import java.util.Iterator;

/**
 * StorageIterator is both an {@link Iterator} and {@link NativeProxy}.
 *
 * <p>Such interface is needed until an automatic resource management is implemented for
 * iterators of all indices. Until then, you have to close your iterators
 * when you're done with them:
 * <pre>
 * {@code
 *
 * try (ListIndexProxy list = new ListIndexProxy(name, view);
 *      StorageIterator<E> iterator = list.iterator()) {
 *   while (iterator.hasNext()) {
 *     E element = iterator.next();
 *     process(element);
 *   }
 *   // both index and iterator will be automatically closed
 *   // when they leave try-with-resources scope.
 * }
 * }
 * </pre>
 * @param <E> type of the entry.
 */
public interface StorageIterator<E> extends Iterator<E>, NativeProxy {}
