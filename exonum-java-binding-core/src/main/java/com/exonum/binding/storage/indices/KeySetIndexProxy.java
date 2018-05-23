package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.StoragePreconditions.checkIdInGroup;
import static com.exonum.binding.storage.indices.StoragePreconditions.checkIndexName;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.NativeHandle;
import com.exonum.binding.proxy.ProxyDestructor;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.serialization.CheckingSerializerDecorator;
import com.exonum.binding.storage.serialization.Serializer;
import java.util.Iterator;
import java.util.function.LongSupplier;

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
 * <p>All method arguments are non-null by default.
 *
 * <p>This class is not thread-safe and and its instances shall not be shared between threads.
 *
 * <p>When the view goes out of scope, this set is destroyed. Subsequent use of the closed set
 * is prohibited and will result in {@link IllegalStateException}.
 *
 * @param <E> the type of elements in this set
 * @see ValueSetIndexProxy
 * @see View
 */
public class KeySetIndexProxy<E> extends AbstractIndexProxy {

  private final CheckingSerializerDecorator<E> serializer;

  /**
   * Creates a new key set proxy.
   *
   * @param name a unique alphanumeric non-empty identifier of this set in the underlying storage:
   *             [a-zA-Z0-9_]
   * @param view a database view. Must be valid. If a view is read-only,
   *             "destructive" operations are not permitted.
   * @param serializer a serializer of set keys
   * @param <E> the type of keys in this set
   * @throws IllegalStateException if the view is not valid
   * @throws IllegalArgumentException if the name is empty
   * @throws NullPointerException if any argument is null
   */
  public static <E> KeySetIndexProxy<E> newInstance(
      String name, View view, Serializer<E> serializer) {
    checkIndexName(name);
    CheckingSerializerDecorator<E> s = CheckingSerializerDecorator.from(serializer);

    long viewNativeHandle = view.getViewNativeHandle();
    NativeHandle setNativeHandle = createNativeSet(view,
        () -> nativeCreate(name, viewNativeHandle));

    return new KeySetIndexProxy<>(setNativeHandle, name, view, s);
  }

  /**
   * Creates a new key set in a <a href="package-summary.html#families">collection group</a>
   * with the given name.
   *
   * <p>See a <a href="package-summary.html#families-limitations">caveat</a> on index identifiers.
   *
   * @param groupName a name of the collection group
   * @param indexId an identifier of this collection in the group, see the caveats
   * @param view a database view
   * @param serializer a serializer of set keys
   * @param <E> the type of keys in this set
   * @return a new key set
   * @throws IllegalStateException if the view is not valid
   * @throws IllegalArgumentException if the name or index id is empty
   */
  public static <E> KeySetIndexProxy<E> newInGroupUnsafe(String groupName, byte[] indexId,
                                                         View view, Serializer<E> serializer) {
    checkIndexName(groupName);
    checkIdInGroup(indexId);
    CheckingSerializerDecorator<E> s = CheckingSerializerDecorator.from(serializer);

    long viewNativeHandle = view.getViewNativeHandle();
    NativeHandle setNativeHandle = createNativeSet(view,
        () -> nativeCreateInGroup(groupName, indexId, viewNativeHandle));

    return new KeySetIndexProxy<>(setNativeHandle, groupName, view, s);
  }

  private static NativeHandle createNativeSet(View view, LongSupplier nativeSetConstructor) {
    Cleaner cleaner = view.getCleaner();
    NativeHandle setNativeHandle = new NativeHandle(nativeSetConstructor.getAsLong());
    ProxyDestructor.newRegistered(cleaner, setNativeHandle, KeySetIndexProxy.class,
        KeySetIndexProxy::nativeFree);
    return setNativeHandle;
  }

  private KeySetIndexProxy(NativeHandle nativeHandle, String name, View view,
                           CheckingSerializerDecorator<E> serializer) {
    super(nativeHandle, name, view);
    this.serializer = serializer;
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
  public void add(E e) {
    notifyModified();
    byte[] dbElement = serializer.toBytes(e);
    nativeAdd(getNativeHandle(), dbElement);
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
  public boolean contains(E e) {
    byte[] dbElement = serializer.toBytes(e);
    return nativeContains(getNativeHandle(), dbElement);
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
  public Iterator<E> iterator() {
    return StorageIterators.createIterator(
        nativeCreateIterator(getNativeHandle()),
        this::nativeIteratorNext,
        this::nativeIteratorFree,
        dbView,
        modCounter,
        serializer::fromBytes);
  }

  /**
   * Removes the element from this set. If it's not in the set, does nothing.
   * 
   * @param e an element to remove.
   * @throws NullPointerException if the element is null
   * @throws IllegalStateException if this set is not valid
   * @throws UnsupportedOperationException if this set is read-only
   */
  public void remove(E e) {
    notifyModified();
    byte[] dbElement = serializer.toBytes(e);
    nativeRemove(getNativeHandle(), dbElement);
  }

  private static native long nativeCreate(String setName, long viewNativeHandle);

  private static native long nativeCreateInGroup(String groupName, byte[] setId,
                                                 long viewNativeHandle);

  private native void nativeAdd(long nativeHandle, byte[] e);

  private native void nativeClear(long nativeHandle);

  private native boolean nativeContains(long nativeHandle, byte[] e);

  private native long nativeCreateIterator(long nativeHandle);

  private native byte[] nativeIteratorNext(long iterNativeHandle);

  private native void nativeIteratorFree(long iterNativeHandle);

  private native void nativeRemove(long nativeHandle, byte[] e);

  private static native void nativeFree(long nativeHandle);
}
