package com.exonum.binding.proxy;

import static com.exonum.binding.proxy.StoragePreconditions.checkIndexPrefix;
import static com.exonum.binding.proxy.StoragePreconditions.checkStorageKey;
import static com.exonum.binding.proxy.StoragePreconditions.checkStorageValue;
import static com.exonum.binding.proxy.StoragePreconditions.checkValid;

import com.exonum.binding.annotations.ImproveDocs;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@ImproveDocs(
    assignee = "dt",
    reason = "consider using exonum::storage docs + java.util.Map as a reference"
)
public class MapIndexProxy extends AbstractNativeProxy {
  // TODO: consider moving 'dbView' to a super class as 'parents'
  //       (= objects that must not be deleted before this)
  private final View dbView;

  @ImproveDocs(assignee = "dt")
  public MapIndexProxy(View view, byte[] prefix) {
    super(nativeCreate(view.getNativeHandle(), checkIndexPrefix(prefix)),
        true);
    this.dbView = view;
  }

  public void put(byte[] key, byte[] value) {
    nativePut(checkStorageKey(key), checkStorageValue(value), getNativeHandle());
  }

  public byte[] get(byte[] key) {
    return nativeGet(checkStorageKey(key), getNativeHandle());
  }

  public void remove(byte[] key) {
    nativeRemove(checkStorageKey(key), getNativeHandle());
  }

  /**
   * Returns an iterator over keys. Must be closed.
   */
  // TODO(dt): consider creating a subclass (RustByteIter) so that you don't have to put a
  // type parameter?
  public RustIter<byte[]> keys() {
    return new ConfigurableIter<>(nativeCreateKeysIter(getNativeHandle()),
        this::nativeKeysIterNext,
        this::nativeKeysIterFree);
  }

  /**
   * Returns an iterator over values. Must be closed.
   */
  public RustIter<byte[]> values() {
    return new ConfigurableIter<>(nativeCreateValuesIter(getNativeHandle()),
          this::nativeValuesIterNext,
          this::nativeValuesIterFree);
  }

  public void clear() {
    nativeClear(getNativeHandle());
  }

  @Override
  void disposeInternal() {
    checkValid(dbView);
    nativeFree(getNativeHandle());
  }

  private static native long nativeCreate(long viewNativeHandle, byte[] prefix);

  private native void nativePut(byte[] key, byte[] value, long nativeHandle);

  private native byte[] nativeGet(byte[] key, long nativeHandle);

  private native void nativeRemove(byte[] key, long nativeHandle);

  private native long nativeCreateKeysIter(long nativeHandle);

  private native byte[] nativeKeysIterNext(long iterNativeHandle);

  private native void nativeKeysIterFree(long iterNativeHandle);

  private native long nativeCreateValuesIter(long nativeHandle);

  private native byte[] nativeValuesIterNext(long iterNativeHandle);

  private native void nativeValuesIterFree(long iterNativeHandle);

  private native void nativeClear(long nativeHandle);

  private native void nativeFree(long nativeHandle);

  private class ConfigurableIter<E> extends RustIter<E> {

    private final Function<Long, E> nextFunction;
    private final Consumer<Long> disposeOperation;

    ConfigurableIter(long nativeHandle,
                     Function<Long, E> nextFunction,
                     Consumer<Long> disposeOperation) {
      super(nativeHandle, true);
      this.nextFunction = nextFunction;
      this.disposeOperation = disposeOperation;
    }

    @Override
    public Optional<E> next() {
      // TODO(dt): check for concurrent modifications of the map.
      return Optional.ofNullable(nextFunction.apply(getNativeHandle()));
    }

    @Override
    void disposeInternal() {
      checkValid(MapIndexProxy.this);
      disposeOperation.accept(getNativeHandle());
    }
  }
}
