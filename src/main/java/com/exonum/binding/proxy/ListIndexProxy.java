package com.exonum.binding.proxy;

import static com.exonum.binding.proxy.StoragePreconditions.checkCanModify;
import static com.exonum.binding.proxy.StoragePreconditions.checkElementIndex;
import static com.exonum.binding.proxy.StoragePreconditions.checkIndexPrefix;
import static com.exonum.binding.proxy.StoragePreconditions.checkStorageValue;
import static com.exonum.binding.proxy.StoragePreconditions.checkValid;
import static com.google.common.base.Preconditions.checkArgument;

import java.util.NoSuchElementException;

/**
 * A list index proxy is a contiguous list of elements.
 * Elements may be added to or removed from the end of the list only.
 *
 * <p>The "destructive" methods of the list, i.e., those that change its contents,
 * are specified to throw {@link UnsupportedOperationException} if
 * this list has been created with a read-only database view.
 *
 * <p>This list implementation does not permit null elements.
 *
 * <p>As any native proxy, this list <em>must be closed</em> when no longer needed.
 * Subsequent use of the closed list is prohibited and will result in {@link IllegalStateException}.
 */
public class ListIndexProxy extends AbstractNativeProxy {

  private final View dbView;

  private final ViewModificationCounter modCounter;

  /**
   * Creates a new ListIndexProxy.
   *
   * @param view a database view. Must be valid.
   *             If a view is read-only, "destructive" operations are not permitted.
   * @param prefix a unique identifier of this list in the underlying storage
   * @throws IllegalStateException if the view is not valid
   * @throws IllegalArgumentException if the prefix has zero size
   * @throws NullPointerException if any argument is null
   */
  ListIndexProxy(View view, byte[] prefix) {
    super(nativeCreate(view.getNativeHandle(), checkIndexPrefix(prefix)), true);
    this.dbView = view;
    modCounter = ViewModificationCounter.getInstance();
  }

  /**
   * Adds a new element to the end of the list.
   *
   * @param e an element to append to the list.
   * @throws NullPointerException if the element is null
   * @throws IllegalStateException if this list is not valid
   * @throws UnsupportedOperationException if this list is read-only
   */
  public void add(byte[] e) {
    notifyModified();
    nativeAdd(getNativeHandle(), checkStorageValue(e));
  }

  /**
   * Replaces the element at the given index of the list with the specified element.
   *
   * @param index an index of the element to replace.
   * @param e an element to add.
   * @throws IndexOutOfBoundsException if the index is invalid.
   * @throws NullPointerException if the element is null
   * @throws IllegalStateException if this list is not valid
   * @throws UnsupportedOperationException if this list is read-only
   */
  public void set(long index, byte[] e) {
    checkElementIndex(index, size());
    notifyModified();
    nativeSet(getNativeHandle(), index, checkStorageValue(e));
  }

  /**
   * Returns the element at the given index.
   *
   * @param index an index of the element to return.
   * @return an element at the given index.
   * @throws IndexOutOfBoundsException if index is invalid.
   * @throws IllegalStateException if this list is not valid
   */
  public byte[] get(long index) {
    return nativeGet(getNativeHandle(), checkElementIndex(index, size()));
  }

  /**
   * Returns the last element of the list.
   *
   * @return the last element of the list.
   * @throws NoSuchElementException if the list is empty.
   * @throws IllegalStateException if this list is not valid
   */
  public byte[] getLast() {
    byte[] e = nativeGetLast(getNativeHandle());
    if (e == null) {
      throw new NoSuchElementException("List is empty");
    }
    return e;
  }

  /**
   * Removes the last element of the list and returns it.
   *
   * @return the last element of the list.
   * @throws NoSuchElementException if the list is empty
   * @throws IllegalStateException if this list is not valid
   * @throws UnsupportedOperationException if this list is read-only
   */
  public byte[] removeLast() {
    notifyModified();
    byte[] e = nativeRemoveLast(getNativeHandle());
    if (e == null) {
      throw new NoSuchElementException("List is empty");
    }
    return e;
  }

  /**
   * Truncates the list, reducing its size to {@code newSize}.
   *
   * <p>If {@code newSize < size()}, keeps the first {@code newSize} elements, removing the rest.
   * If {@code newSize >= size()}, has no effect.
   *
   * @param newSize the maximum number of elements to keep
   * @throws IllegalArgumentException if the new size is negative
   * @throws IllegalStateException if this list is not valid
   * @throws UnsupportedOperationException if this list is read-only
   */
  public void truncate(long newSize) {
    checkArgument(newSize >= 0, "New size must be non-negative: " + newSize);
    notifyModified();
    nativeTruncate(getNativeHandle(), newSize);
  }

  /**
   * Clears the list.
   *
   * @throws IllegalStateException if this list is not valid
   * @throws UnsupportedOperationException if this list is read-only
   */
  public void clear() {
    notifyModified();
    nativeClear(getNativeHandle());
  }

  private void notifyModified() {
    modCounter.notifyModified(checkCanModify(dbView));
  }

  /**
   * Returns true if the list is empty, false — otherwise.
   *
   * @throws IllegalStateException if this list is not valid
   */
  public boolean isEmpty() {
    return nativeIsEmpty(getNativeHandle());
  }

  /**
   * Returns the number of elements in the list.
   *
   * @throws IllegalStateException if this list is not valid
   */
  public long size() {
    return nativeSize(getNativeHandle());
  }

  /**
   * Returns an iterator over the elements of the list.
   *
   * <p>Any destructive operation on the same {@link Fork} this list uses
   * (but not necessarily on <em>this list</em>) will invalidate the iterator.
   *
   * @throws IllegalStateException if this list is not valid
   */
  public RustIter<byte[]> iterator() {
    return new ConfigurableRustIter<>(nativeCreateIter(getNativeHandle()),
        this::nativeIterNext,
        this::nativeIterFree,
        dbView,
        modCounter);
  }

  @Override
  void disposeInternal() {
    checkValid(dbView);
    nativeFree(getNativeHandle());
  }

  private static native long nativeCreate(long viewNativeHandle, byte[] listPrefix);

  private native void nativeFree(long nativeHandle);

  private native void nativeAdd(long nativeHandle, byte[] e);

  private native void nativeSet(long nativeHandle, long index, byte[] e);

  private native byte[] nativeGet(long nativeHandle, long index);

  private native byte[] nativeGetLast(long nativeHandle);

  private native byte[] nativeRemoveLast(long nativeHandle);

  private native void nativeTruncate(long nativeHandle, long newSize);

  private native void nativeClear(long nativeHandle);

  private native boolean nativeIsEmpty(long nativeHandle);

  private native long nativeSize(long nativeHandle);

  private native long nativeCreateIter(long nativeHandle);

  private native byte[] nativeIterNext(long iterNativeHandle);

  private native void nativeIterFree(long iterNativeHandle);

}
