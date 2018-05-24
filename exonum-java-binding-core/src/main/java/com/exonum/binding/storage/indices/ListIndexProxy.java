package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.StoragePreconditions.checkIndexName;
import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.NativeHandle;
import com.exonum.binding.proxy.ProxyDestructor;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.serialization.CheckingSerializerDecorator;
import com.exonum.binding.storage.serialization.Serializer;
import java.util.NoSuchElementException;

/**
 * A list index proxy is a contiguous list of elements.
 * Elements may be added to or removed from the end of the list only.
 *
 * <p>This list implementation does not permit null elements.
 *
 * <p>The "destructive" methods of the list, i.e., those that change its contents,
 * are specified to throw {@link UnsupportedOperationException} if
 * this list has been created with a read-only database view.
 *
 * <p>All method arguments are non-null by default.
 *
 * <p>This class is not thread-safe and and its instances shall not be shared between threads.
 *
 * <p>When the view goes out of scope, this list is destroyed. Subsequent use of the closed list
 * is prohibited and will result in {@link IllegalStateException}.
 *
 * @param <E> the type of elements in this list
 * @see View
 */
public final class ListIndexProxy<E> extends AbstractListIndexProxy<E> implements ListIndex<E> {

  /**
   * Creates a new ListIndexProxy.
   *
   * @param name a unique alphanumeric non-empty identifier of this list in the underlying storage:
   *             [a-zA-Z0-9_]
   * @param view a database view. Must be valid.
   *             If a view is read-only, "destructive" operations are not permitted.
   * @param serializer a serializer of elements
   * @throws IllegalStateException if the view is not valid
   * @throws IllegalArgumentException if the name is empty
   * @throws NullPointerException if any argument is null
   */
  public static <E> ListIndexProxy<E> newInstance(
      String name, View view, Serializer<E> serializer) {
    checkIndexName(name);
    CheckingSerializerDecorator<E> s = CheckingSerializerDecorator.from(serializer);

    NativeHandle listNativeHandle = createNativeList(name, view);

    return new ListIndexProxy<>(listNativeHandle, name, view, s);
  }

  private static NativeHandle createNativeList(String name, View view) {
    long viewNativeHandle = view.getViewNativeHandle();
    NativeHandle listNativeHandle = new NativeHandle(nativeCreate(name, viewNativeHandle));

    Cleaner cleaner = view.getCleaner();
    ProxyDestructor.newRegistered(cleaner, listNativeHandle, ListIndexProxy.class,
        ListIndexProxy::nativeFree);
    return listNativeHandle;
  }

  private ListIndexProxy(NativeHandle nativeHandle, String name, View view,
                         CheckingSerializerDecorator<E> serializer) {
    super(nativeHandle, name, view, serializer);
  }

  /**
   * Removes the last element of the list and returns it.
   *
   * @return the last element of the list.
   * @throws NoSuchElementException if the list is empty
   * @throws IllegalStateException if this list is not valid
   * @throws UnsupportedOperationException if this list is read-only
   */
  public E removeLast() {
    notifyModified();
    byte[] e = nativeRemoveLast(getNativeHandle());
    if (e == null) {
      throw new NoSuchElementException("List is empty");
    }
    return serializer.fromBytes(e);
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
    checkArgument(newSize >= 0, "New size must be non-negative: %s", newSize);
    notifyModified();
    nativeTruncate(getNativeHandle(), newSize);
  }

  private static native long nativeCreate(String listName, long viewNativeHandle);

  private static native void nativeFree(long nativeHandle);

  @Override
  native void nativeAdd(long nativeHandle, byte[] e);

  @Override
  native void nativeSet(long nativeHandle, long index, byte[] e);

  @Override
  native byte[] nativeGet(long nativeHandle, long index);

  @Override
  native byte[] nativeGetLast(long nativeHandle);

  native byte[] nativeRemoveLast(long nativeHandle);

  native void nativeTruncate(long nativeHandle, long newSize);

  @Override
  native void nativeClear(long nativeHandle);

  @Override
  native boolean nativeIsEmpty(long nativeHandle);

  @Override
  native long nativeSize(long nativeHandle);

  @Override
  native long nativeCreateIter(long nativeHandle);

  @Override
  native byte[] nativeIterNext(long iterNativeHandle);

  @Override
  native void nativeIterFree(long iterNativeHandle);
}
