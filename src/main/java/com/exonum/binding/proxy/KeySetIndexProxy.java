package com.exonum.binding.proxy;

import static com.exonum.binding.proxy.StoragePreconditions.checkIndexPrefix;
import static com.exonum.binding.proxy.StoragePreconditions.checkStorageKey;
import static com.exonum.binding.proxy.StoragePreconditions.checkValid;

/**
 * A key set is an index that contains no duplicate elements (keys).
 * This implementation does not permit null elements.
 *
 * <p>The elements are stored as keys in the underlying database in the lexicographical order.
 * As each operation accepting an element needs to pass the <em>entire</em> element
 * to the underlying database as a key, it's better, in terms of performance, to use this index
 * with small elements. If you need to store large elements and can perform operations
 * by hashes of the elements, consider using a {@link ValueSetIndexProxy}.
 *
 * <p>The "destructive" methods of the set, i.e., the ones that change its contents,
 * are specified to throw {@link UnsupportedOperationException} if the set has been created with
 * a read-only database view.
 *
 * <p>As any native proxy, the set <em>must be closed</em> when no longer needed.
 * Subsequent use of the closed set is prohibited and will result in {@link IllegalStateException}.
 *
 * @see ValueSetIndexProxy
 */
public class KeySetIndexProxy extends AbstractIndexProxy {

  /**
   * Creates a new key set proxy.
   *
   * @param prefix a set prefix — a unique identifier of this set in the underlying storage
   * @param view a database view. Must be valid. If a view is read-only,
   *             "destructive" operations are not permitted.
   * @throws IllegalStateException if the view is not valid
   * @throws IllegalArgumentException if the prefix has zero size
   * @throws NullPointerException if any argument is null
   */
  KeySetIndexProxy(byte[] prefix, View view) {
    super(nativeCreate(checkIndexPrefix(prefix), view.getNativeHandle()), view);
  }

  /**
   * Adds a new element to the set. The method has no effect if
   * the set already contains such element.
   *
   * @param e an element to add
   * @throws NullPointerException if the element is null
   * @throws IllegalStateException if this set is not valid
   * @throws UnsupportedOperationException if this set is read-only
   */
  public void add(byte[] e) {
    notifyModified();
    nativeAdd(getNativeHandle(), checkStorageKey(e));
  }

  /**
   * Removes all of the elements from this set.
   * The set will be empty after this method returns.
   *
   * @throws IllegalStateException if this set is not valid
   * @throws UnsupportedOperationException if this set is read-only
   */
  public void clear() {
    notifyModified();
    nativeClear(getNativeHandle());
  }
  
  /**
   * Returns true if this set contains the specified element.
   *
   * @throws NullPointerException if the element is null
   * @throws IllegalStateException if this set is not valid
   */
  public boolean contains(byte[] e) {
    return nativeContains(getNativeHandle(), checkStorageKey(e));
  }

  /**
   * Creates an iterator over the set elements. The elements are ordered lexicographically.
   * 
   * <p>Any destructive operation on the same {@link Fork} this set uses 
   * (but not necessarily on <em>this set</em>) will invalidate the iterator.
   * 
   * @return an iterator over the elements of this set
   * @throws IllegalStateException if this set is not valid 
   */
  public StorageIterator<byte[]> iterator() {
    return StorageIterators.createIterator(
        nativeCreateIterator(getNativeHandle()),
        this::nativeIteratorNext,
        this::nativeIteratorFree,
        dbView,
        modCounter);
  }

  /**
   * Removes the element from this set. If it's not in the set, does nothing.
   * 
   * @param e an element to remove.
   * @throws NullPointerException if the element is null
   * @throws IllegalStateException if this set is not valid
   * @throws UnsupportedOperationException if this set is read-only
   */
  public void remove(byte[] e) {
    notifyModified();
    nativeRemove(getNativeHandle(), checkStorageKey(e));
  }

  @Override
  void disposeInternal() {
    checkValid(dbView);
    nativeFree(getNativeHandle());
  }

  private static native long nativeCreate(byte[] setPrefix, long viewNativeHandle);

  private native void nativeAdd(long nativeHandle, byte[] e);

  private native void nativeClear(long nativeHandle);

  private native boolean nativeContains(long nativeHandle, byte[] e);

  private native long nativeCreateIterator(long nativeHandle);

  private native byte[] nativeIteratorNext(long iterNativeHandle);

  private native void nativeIteratorFree(long iterNativeHandle);

  private native void nativeRemove(long nativeHandle, byte[] e);

  private native void nativeFree(long nativeHandle);
}
