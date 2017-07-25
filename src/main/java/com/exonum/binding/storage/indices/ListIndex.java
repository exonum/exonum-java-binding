package com.exonum.binding.storage.indices;

import com.exonum.binding.proxy.NativeProxy;
import com.exonum.binding.storage.database.Fork;
import java.util.NoSuchElementException;

/**
 * A list index proxy is a contiguous list of elements.
 *
 * <p>The "destructive" methods of the list, i.e., those that change its contents,
 * are specified to throw {@link UnsupportedOperationException} if
 * this list has been created with a read-only database view.
 *
 * <p>This interface prohibits null elements.
 *
 * <p>As any native proxy, the list index <em>must be closed</em> when no longer needed.
 * Subsequent use of the closed list is prohibited and will result in {@link IllegalStateException}.
 */
public interface ListIndex extends NativeProxy {

  /**
   * Adds a new element to the end of the list.
   *
   * @param e an element to append to the list
   * @throws NullPointerException if the element is null
   * @throws IllegalStateException if this list is not valid
   * @throws UnsupportedOperationException if this list is read-only
   */
  void add(byte[] e);

  /**
   * Replaces the element at the given index of the list with the specified element.
   *
   * @param index an index of the element to replace
   * @param e an element to add
   * @throws IndexOutOfBoundsException if the index is invalid
   * @throws NullPointerException if the element is null
   * @throws IllegalStateException if this list is not valid
   * @throws UnsupportedOperationException if this list is read-only
   */
  void set(long index, byte[] e);

  /**
   * Returns the element at the given index.
   *
   * @param index an index of the element to return
   * @return an element at the given index
   * @throws IndexOutOfBoundsException if index is invalid
   * @throws IllegalStateException if this list is not valid
   */
  byte[] get(long index);

  /**
   * Returns the last element of the list.
   *
   * @return the last element of the list
   * @throws NoSuchElementException if the list is empty
   * @throws IllegalStateException if this list is not valid
   */
  byte[] getLast();

  /**
   * Clears the list.
   *
   * @throws IllegalStateException if this list is not valid
   * @throws UnsupportedOperationException if this list is read-only
   */
  void clear();

  /**
   * Returns true if the list is empty, false — otherwise.
   *
   * @throws IllegalStateException if this list is not valid
   */
  boolean isEmpty();

  /**
   * Returns the number of elements in the list.
   *
   * @throws IllegalStateException if this list is not valid
   */
  long size();

  /**
   * Returns an iterator over the elements of the list.
   *
   * <p>Any destructive operation on the same {@link Fork} this list uses
   * (but not necessarily on <em>this list</em>) will invalidate the iterator.
   *
   * @throws IllegalStateException if this list is not valid
   */
  StorageIterator<byte[]> iterator();
}
