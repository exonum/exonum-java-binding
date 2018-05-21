package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.StoragePreconditions.checkIndexName;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.NativeHandle;
import com.exonum.binding.proxy.ProxyDestructor;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.serialization.CheckingSerializerDecorator;
import com.exonum.binding.storage.serialization.Serializer;
import java.util.Iterator;

/**
 * A MapIndex is an index that maps keys to values. A map cannot contain duplicate keys;
 * each key corresponds to at most one value.
 *
 * <p>The map implementation does not permit null keys and values.
 *
 * <p>The "destructive" methods of the map, i.e., the one that change the map contents,
 * are specified to throw {@link UnsupportedOperationException} if
 * the map has been created with a read-only database view.
 *
 * <p>All method arguments are non-null by default.
 *
 * <p>This class is not thread-safe and its instances shall not be shared between threads.
 *
 * <p>When the view goes out of scope, this map is destroyed. Subsequent use of the closed map
 * is prohibited and will result in {@link IllegalStateException}.
 *
 * @param <K> the type of keys in this map
 * @param <V> the type of values in this map
 * @see View
 */
public class MapIndexProxy<K, V> extends AbstractIndexProxy implements MapIndex<K, V> {

  private final CheckingSerializerDecorator<K> keySerializer;
  private final CheckingSerializerDecorator<V> valueSerializer;

  /**
   * Creates a new MapIndexProxy.
   *
   * @param name a unique alphanumeric non-empty identifier of this map in the underlying storage:
   *             [a-zA-Z0-9_]
   * @param view a database view. Must be valid.
   *             If a view is read-only, "destructive" operations are not permitted.
   * @param keySerializer a serializer of keys
   * @param valueSerializer a serializer of values
   * @throws IllegalStateException if the view is not valid
   * @throws IllegalArgumentException if the name is empty
   */
  public static <K, V> MapIndexProxy<K, V> newInstance(String name, View view,
                                                       Serializer<K> keySerializer,
                                                       Serializer<V> valueSerializer) {
    checkIndexName(name);
    CheckingSerializerDecorator<K> ks = CheckingSerializerDecorator.from(keySerializer);
    CheckingSerializerDecorator<V> vs = CheckingSerializerDecorator.from(valueSerializer);

    NativeHandle mapNativeHandle = createNativeMap(name, view);

    return new MapIndexProxy<>(mapNativeHandle, name, view, ks, vs);
  }

  private static NativeHandle createNativeMap(String name, View view) {
    long viewNativeHandle = view.getViewNativeHandle();
    NativeHandle mapNativeHandle = new NativeHandle(nativeCreate(name, viewNativeHandle));

    Cleaner cleaner = view.getCleaner();
    ProxyDestructor.newRegistered(cleaner, mapNativeHandle, MapIndexProxy.class,
        MapIndexProxy::nativeFree);
    return mapNativeHandle;
  }

  private MapIndexProxy(NativeHandle nativeHandle, String name, View view,
                        CheckingSerializerDecorator<K> keySerializer,
                        CheckingSerializerDecorator<V> valueSerializer) {
    super(nativeHandle, name, view);
    this.keySerializer = keySerializer;
    this.valueSerializer = valueSerializer;
  }

  @Override
  public boolean containsKey(K key) {
    byte[] dbKey = keySerializer.toBytes(key);
    return nativeContainsKey(getNativeHandle(), dbKey);
  }

  @Override
  public void put(K key, V value) {
    notifyModified();
    byte[] dbKey = keySerializer.toBytes(key);
    byte[] dbValue = valueSerializer.toBytes(value);
    nativePut(getNativeHandle(), dbKey, dbValue);
  }

  @Override
  public V get(K key) {
    byte[] dbKey = keySerializer.toBytes(key);
    byte[] dbValue = nativeGet(getNativeHandle(), dbKey);
    return (dbValue == null) ? null : valueSerializer.fromBytes(dbValue);
  }

  @Override
  public void remove(K key) {
    notifyModified();
    byte[] dbKey = keySerializer.toBytes(key);
    nativeRemove(getNativeHandle(), dbKey);
  }

  @Override
  public Iterator<K> keys() {
    return StorageIterators.createIterator(
        nativeCreateKeysIter(getNativeHandle()),
        this::nativeKeysIterNext,
        this::nativeKeysIterFree,
        dbView,
        modCounter,
        keySerializer::fromBytes
    );
  }

  @Override
  public Iterator<V> values() {
    return StorageIterators.createIterator(
        nativeCreateValuesIter(getNativeHandle()),
        this::nativeValuesIterNext,
        this::nativeValuesIterFree,
        dbView,
        modCounter,
        valueSerializer::fromBytes
    );
  }

  @Override
  public Iterator<MapEntry<K, V>> entries() {
    return StorageIterators.createIterator(
        nativeCreateEntriesIter(getNativeHandle()),
        this::nativeEntriesIterNext,
        this::nativeEntriesIterFree,
        dbView,
        modCounter,
        (entry) -> MapEntry.fromInternal(entry, keySerializer, valueSerializer)
    );
  }

  private native long nativeCreateEntriesIter(long nativeHandle);

  private native MapEntryInternal nativeEntriesIterNext(long iterNativeHandle);

  private native void nativeEntriesIterFree(long iterNativeHandle);

  @Override
  public void clear() {
    notifyModified();
    nativeClear(getNativeHandle());
  }

  private static native long nativeCreate(String name, long viewNativeHandle);

  private native boolean nativeContainsKey(long nativeHandle, byte[] key);

  private native void nativePut(long nativeHandle, byte[] key, byte[] value);

  private native byte[] nativeGet(long nativeHandle, byte[] key);

  private native void nativeRemove(long nativeHandle, byte[] key);

  private native long nativeCreateKeysIter(long nativeHandle);

  private native byte[] nativeKeysIterNext(long iterNativeHandle);

  private native void nativeKeysIterFree(long iterNativeHandle);

  private native long nativeCreateValuesIter(long nativeHandle);

  private native byte[] nativeValuesIterNext(long iterNativeHandle);

  private native void nativeValuesIterFree(long iterNativeHandle);

  private native void nativeClear(long nativeHandle);

  private static native void nativeFree(long nativeHandle);

}
