package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.StoragePreconditions.checkIndexName;
import static com.exonum.binding.storage.indices.StoragePreconditions.checkStorageValue;
import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.storage.database.ForkProxy;
import com.exonum.binding.storage.database.ViewProxy;
import com.exonum.binding.storage.serialization.CheckingSerializerDecorator;
import com.exonum.binding.storage.serialization.Serializer;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.MustBeClosed;
import javax.annotation.Nullable;

/**
 * A value set is an index that contains no duplicate elements (values).
 * This implementation does not permit null elements.
 *
 * <p>The elements are stored in the underlying database as values,
 * whereas their cryptographic hashes are used as keys, making this set implementation
 * more suitable for storing large elements. If your application has <em>small</em> elements and
 * does not need to perform set operations by hashes of the elements,
 * consider using a {@link KeySetIndexProxy}.
 *
 * <p>The "destructive" methods of the set, i.e., the ones that change its contents,
 * are specified to throw {@link UnsupportedOperationException} if the set has been created with
 * a read-only database view.
 *
 * <p>All method arguments are non-null by default.
 *
 * <p>This class is not thread-safe and and its instances shall not be shared between threads.
 *
 * <p>As any native proxy, the set <em>must be closed</em> when no longer needed.
 * Subsequent use of the closed set is prohibited and will result in {@link IllegalStateException}.
 *
 * @param <E> the type of elements in this set
 * @see KeySetIndexProxy
 * @see ViewProxy
 */
public class ValueSetIndexProxy<E> extends AbstractIndexProxy {

  private final CheckingSerializerDecorator<E> serializer;

  /**
   * Creates a new value set proxy.
   *
   * @param name a unique alphanumeric non-empty identifier of this set in the underlying storage:
   *             [a-zA-Z0-9_]
   * @param view a database view. Must be valid. If a view is read-only,
   *             "destructive" operations are not permitted.
   * @param serializer a serializer of values
   * @throws IllegalStateException if the view is not valid
   * @throws IllegalArgumentException if the name is empty
   * @throws NullPointerException if any argument is null
   */
  public ValueSetIndexProxy(String name, ViewProxy view, Serializer<E> serializer) {
    super(nativeCreate(checkIndexName(name), view.getViewNativeHandle()), name, view);
    this.serializer = CheckingSerializerDecorator.from(serializer);
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
   * @see #containsByHash(HashCode)
   */
  public boolean contains(E e) {
    byte[] dbElement = serializer.toBytes(e);
    return nativeContains(getNativeHandle(), dbElement);
  }

  /**
   * Returns true if this set contains an element with the specified hash.
   *
   * @param elementHash a hash of an element
   * @throws NullPointerException if the hash is null
   * @throws IllegalStateException if this set is not valid
   */
  public boolean containsByHash(HashCode elementHash) {
    return nativeContainsByHash(getNativeHandle(), elementHash.asBytes());
  }

  /**
   * Creates an iterator over the hashes of the elements in this set.
   * The hashes are ordered lexicographically.
   *
   * <p>Any destructive operation on the same {@link ForkProxy} this set uses
   * (but not necessarily on <em>this set</em>) will invalidate the iterator.
   *
   * @return an iterator over the hashes of the elements in this set
   * @throws IllegalStateException if this set is not valid
   */
  @MustBeClosed
  public StorageIterator<HashCode> hashes() {
    return StorageIterators.createIterator(
        nativeCreateHashIterator(getNativeHandle()),
        this::nativeHashIteratorNext,
        this::nativeHashIteratorFree,
        this,
        modCounter,
        HashCode::fromBytes);
  }

  /**
   * Returns an iterator over the entries of this set. An entry is a hash-value pair.
   * The entries are ordered by keys lexicographically.
   *
   * <p>Any destructive operation on the same {@link ForkProxy} this set uses
   * (but not necessarily on <em>this set</em>) will invalidate the iterator.
   *
   * @return an iterator over the entries of this set
   * @throws IllegalStateException if this set is not valid
   */
  @MustBeClosed
  public StorageIterator<Entry<E>> iterator() {
    return StorageIterators.createIterator(
        nativeCreateIterator(getNativeHandle()),
        this::nativeIteratorNext,
        this::nativeIteratorFree,
        this,
        modCounter,
        (e) -> Entry.fromInternal(e, serializer));
  }

  private native long nativeCreateIterator(long nativeHandle);

  private native EntryInternal nativeIteratorNext(long iterNativeHandle);

  private native void nativeIteratorFree(long iterNativeHandle);

  /**
   * An entry of a value set index: a hash-value pair.
   *
   * <p>An entry contains <em>a copy</em> of the data in the value set index.
   * It does not reflect the changes made to the index since this entry had been created.
   */
  @AutoValue
  public abstract static class Entry<E> {

    /**
     * Returns a hash of the element of the set.
     */
    public abstract HashCode getHash();

    /**
     * Returns an element of the set.
     */
    public abstract E getValue();

    // Do not include (potentially) large value in the hash code: we already have a SHA-256 hash.
    @Override
    public final int hashCode() {
      return getHash().hashCode();
    }

    private static <E> Entry<E> fromInternal(EntryInternal e, Serializer<E> serializer) {
      HashCode hash = HashCode.fromBytes(e.hash);
      E value = serializer.fromBytes(e.value);
      return from(hash, value);
    }

    @VisibleForTesting
    static <E> Entry<E> from(HashCode hash, E value) {
      return new AutoValue_ValueSetIndexProxy_Entry<>(hash, value);
    }
  }

  /**
   * An internal entry: native API.
   */
  private static class EntryInternal {
    final byte[] hash;
    final byte[] value;

    @SuppressWarnings("unused")  // native API
    private EntryInternal(byte[] hash, byte[] value) {
      this.hash = checkNotNull(hash);
      this.value = checkStorageValue(value);
    }
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

  /**
   * Removes an element from this set by its hash. If there is no such element in the set,
   * does nothing.
   *
   * @param elementHash the hash of an element to remove.
   * @throws NullPointerException if the hash is null
   * @throws IllegalStateException if this set is not valid
   * @throws UnsupportedOperationException if this set is read-only
   */
  public void removeByHash(HashCode elementHash) {
    notifyModified();
    nativeRemoveByHash(getNativeHandle(), elementHash.asBytes());
  }

  @Override
  protected void disposeInternal() {
    nativeFree(getNativeHandle());
  }

  private static native long nativeCreate(String setName, long viewNativeHandle);

  private native void nativeAdd(long nativeHandle, byte[] e);

  private native void nativeClear(long nativeHandle);

  private native boolean nativeContains(long nativeHandle, byte[] e);

  private native boolean nativeContainsByHash(long nativeHandle, byte[] elementHash);

  private native long nativeCreateHashIterator(long nativeHandle);

  @Nullable
  private native byte[] nativeHashIteratorNext(long iterNativeHandle);

  private native void nativeHashIteratorFree(long iterNativeHandle);

  private native void nativeRemove(long nativeHandle, byte[] e);

  private native void nativeRemoveByHash(long nativeHandle, byte[] elementHash);

  private native void nativeFree(long nativeHandle);
}
