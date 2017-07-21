package com.exonum.binding.proxy;

import static com.exonum.binding.proxy.StoragePreconditions.checkIndexPrefix;
import static com.exonum.binding.proxy.StoragePreconditions.checkProofKey;
import static com.exonum.binding.proxy.StoragePreconditions.checkStorageValue;
import static com.exonum.binding.proxy.StoragePreconditions.checkValid;

/**
 * A ProofMapIndexProxy is an index that maps keys to values. A map cannot contain duplicate keys;
 * each key corresponds to at most one value. This map is capable of providing cryptographic proofs
 * that a certain key is mapped to a particular value <em>or</em> that there are no mapping for
 * the key in the map.
 *
 * <p>The "destructive" methods of the map, i.e., the one that change the map contents,
 * are specified to throw {@link UnsupportedOperationException} if
 * the map has been created with a read-only database view.
 *
 * <p>This map is implemented as a Merkle-Patricia tree. It does not permit null keys and values,
 * and requires that keys are 32-byte long.
 *
 * <p>As any native proxy, the map <em>must be closed</em> when no longer needed.
 * Subsequent use of the closed map is prohibited and will result in {@link IllegalStateException}.
 */
public class ProofMapIndexProxy extends AbstractIndexProxy implements MapIndex {

  /**
   * Creates a ProofMapIndexProxy.
   *
   * @param prefix a unique identifier of this map in the underlying storage
   * @param view a database view. Must be valid.
   *             If a view is read-only, "destructive" operations are not permitted.
   * @throws IllegalStateException if the view is not valid
   * @throws IllegalArgumentException if the prefix has zero size
   * @throws NullPointerException if any argument is null
   */
  ProofMapIndexProxy(byte[] prefix, View view) {
    super(nativeCreate(checkIndexPrefix(prefix), view.getNativeHandle()), view);
  }

  private static native long nativeCreate(byte[] prefix, long viewNativeHandle);

  @Override
  public boolean containsKey(byte[] key) {
    return nativeContainsKey(getNativeHandle(), checkProofKey(key));
  }

  private native boolean nativeContainsKey(long nativeHandle, byte[] key);

  /**
   * {@inheritDoc}.
   *
   * @param key a proof map key, must be 32-byte long
   * @param value a storage value to associate with the key
   * @throws NullPointerException if any argument is null
   * @throws IllegalStateException if this map is not valid
   * @throws IllegalArgumentException if the size of the key is not 32 bytes
   * @throws UnsupportedOperationException if this map is read-only
   */
  @Override
  public void put(byte[] key, byte[] value) {
    notifyModified();
    nativePut(getNativeHandle(), checkProofKey(key), checkStorageValue(value));
  }

  private native void nativePut(long nativeHandle, byte[] key, byte[] value);

  @Override
  public byte[] get(byte[] key) {
    return nativeGet(getNativeHandle(), checkProofKey(key));
  }

  private native byte[] nativeGet(long nativeHandle, byte[] key);

  @Override
  public void remove(byte[] key) {
    notifyModified();
    nativeRemove(getNativeHandle(), checkProofKey(key));
  }

  private native void nativeRemove(long nativeHandle, byte[] key);

  @Override
  public RustIter<byte[]> keys() {
    return new ConfigurableRustIter<>(
        nativeCreateKeysIter(getNativeHandle()),
        this::nativeKeysIterNext,
        this::nativeKeysIterFree,
        dbView,
        modCounter
    );
  }

  private native long nativeCreateKeysIter(long nativeHandle);

  private native byte[] nativeKeysIterNext(long iterNativeHandle);

  private native void nativeKeysIterFree(long iterNativeHandle);

  @Override
  public RustIter<byte[]> values() {
    return new ConfigurableRustIter<>(
        nativeCreateValuesIter(getNativeHandle()),
        this::nativeValuesIterNext,
        this::nativeValuesIterFree,
        dbView,
        modCounter);
  }

  private native long nativeCreateValuesIter(long nativeHandle);

  private native byte[] nativeValuesIterNext(long iterNativeHandle);

  private native void nativeValuesIterFree(long iterNativeHandle);

  @Override
  public void clear() {
    notifyModified();
    nativeClear(getNativeHandle());
  }

  private native void nativeClear(long nativeHandle);

  @Override
  void disposeInternal() {
    checkValid(dbView);
    nativeFree(getNativeHandle());
  }

  private native void nativeFree(long nativeHandle);
}
