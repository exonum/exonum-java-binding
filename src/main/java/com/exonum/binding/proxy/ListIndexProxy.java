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
 *
 * <p>Elements may be added to or removed from the end of the list only.
 */
public class ListIndexProxy extends AbstractNativeProxy {

  private final View dbView;

  private final ViewModificationCounter modCounter;

  /**
   * Creates a new ListIndexProxy.
   *
   * @param view a database view.
   * @param prefix a list prefix, a unique identifier.
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
   * @throws IndexOutOfBoundsException if index is invalid.
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
   */
  public byte[] get(long index) {
    return nativeGet(getNativeHandle(), checkElementIndex(index, size()));
  }

  /**
   * Returns the last element of the list.
   *
   * @return the last element of the list.
   * @throws NoSuchElementException if the list is empty.
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
   * @param newSize the maximum number of elements to keep.
   */
  public void truncate(long newSize) {
    checkArgument(newSize >= 0, "New size must be non-negative: " + newSize);
    notifyModified();
    nativeTruncate(getNativeHandle(), newSize);
  }

  /**
   * Clears the list.
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
   */
  public boolean isEmpty() {
    return nativeIsEmpty(getNativeHandle());
  }

  /**
   * Returns the number of elements in the list.
   */
  public long size() {
    return nativeSize(getNativeHandle());
  }

  /**
   * Returns an iterator over elements in the list.
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
