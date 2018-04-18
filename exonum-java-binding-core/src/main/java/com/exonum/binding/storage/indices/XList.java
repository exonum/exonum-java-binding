package com.exonum.binding.storage.indices;

import com.exonum.binding.storage.database.ForkProxy;
import com.exonum.binding.storage.database.ViewProxy;
import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * A list index proxy is a contiguous list of elements.
 *
 * <p>The "destructive" methods of the list, i.e., those that change its contents,
 * are specified to throw {@link UnsupportedOperationException} if
 * this list has been created with a read-only database view.
 *
 * <p>This interface prohibits null elements. All methods are non-null by default
 * and are specified to throw {@link NullPointerException} if a null referenced is passed
 * as an argument.
 *
 * <p>As any exonum collection, the list is valid as long as the corresponding {@link ViewProxy}.
 * Use of an invalid list is prohibited and will result in {@link IllegalStateException}.
 *
 * @param <E> the type of elements in this list
 */
public interface XList<E> /* todo: extends XStorageCollection */ {

  /**
   * Adds a new element to the end of the list.
   *
   * @param e an element to append to the list
   * @throws IllegalStateException if this list is not valid
   * @throws UnsupportedOperationException if this list is read-only
   */
  void add(E e);

  /**
   * Adds all elements from the specified collection to this list.
   *
   * <p>If the collection contains an invalid element, this list is not modified.
   *
   * @param elements elements to add to this list
   * @throws NullPointerException if the collection is null or it contains null elements.
   *                              In this case this collection is not modified.
   * @throws IllegalStateException if this list is not valid
   * @throws UnsupportedOperationException if this list is read-only
   */
  void addAll(Collection<? extends E> elements);

  /**
   * Replaces the element at the given index of the list with the specified element.
   *
   * @param index an index of the element to replace
   * @param e an element to add
   * @throws IndexOutOfBoundsException if the index is invalid
   * @throws IllegalStateException if this list is not valid
   * @throws UnsupportedOperationException if this list is read-only
   */
  void set(long index, E e);

  /**
   * Returns the element at the given index.
   *
   * @param index an index of the element to return
   * @return an element at the given index
   * @throws IndexOutOfBoundsException if index is invalid
   * @throws IllegalStateException if this list is not valid
   */
  E get(long index);

  /**
   * Returns the last element of the list.
   *
   * @return the last element of the list
   * @throws NoSuchElementException if the list is empty
   * @throws IllegalStateException if this list is not valid
   */
  E getLast();

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
   * <p>Any destructive operation on the same {@link ForkProxy} this list uses
   * (but not necessarily on <em>this list</em>) will invalidate the iterator.
   *
   * @throws IllegalStateException if this list is not valid
   */
  StorageIterator<E> iterator();

  /**
   * Removes the last element of the list and returns it. This is an <em>optional</em>
   * operation.
   *
   * @return the last element of the list.
   * @throws NoSuchElementException if the list is empty
   * @throws IllegalStateException if this list is not valid
   * @throws UnsupportedOperationException if this list is read-only or this operation
   *     is not supported
   */
  E removeLast();

  /**
   * Truncates the list, reducing its size to {@code newSize}. This is an <em>optional</em>
   * operation.
   *
   * <p>If {@code newSize < size()}, keeps the first {@code newSize} elements, removing the rest.
   * If {@code newSize >= size()}, has no effect.
   *
   * @param newSize the maximum number of elements to keep
   * @throws IllegalArgumentException if the new size is negative
   * @throws IllegalStateException if this list is not valid
   * @throws UnsupportedOperationException if this list is read-only or this operation
   *     is not supported
   */
  void truncate(long newSize);
}
