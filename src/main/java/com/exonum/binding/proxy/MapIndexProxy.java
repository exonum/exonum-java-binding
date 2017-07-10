package com.exonum.binding.proxy;

import static com.exonum.binding.proxy.StoragePreconditions.checkIndexPrefix;
import static com.exonum.binding.proxy.StoragePreconditions.checkStorageKey;
import static com.exonum.binding.proxy.StoragePreconditions.checkStorageValue;
import static com.exonum.binding.proxy.StoragePreconditions.checkValid;

import com.exonum.binding.annotations.ImproveDocs;
import java.util.ConcurrentModificationException;
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
    notifyModified();
    nativePut(checkStorageKey(key), checkStorageValue(value), getNativeHandle());
  }

  public byte[] get(byte[] key) {
    return nativeGet(checkStorageKey(key), getNativeHandle());
  }

  public void remove(byte[] key) {
    notifyModified();
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
        this::nativeKeysIterFree,
        ViewModificationCounter.getInstance());
  }

  /**
   * Returns an iterator over values. Must be closed.
   */
  public RustIter<byte[]> values() {
    return new ConfigurableIter<>(nativeCreateValuesIter(getNativeHandle()),
          this::nativeValuesIterNext,
          this::nativeValuesIterFree,
          ViewModificationCounter.getInstance());
  }

  public void clear() {
    notifyModified();
    nativeClear(getNativeHandle());
  }

  private void notifyModified() {
    ViewModificationCounter.getInstance().notifyModified(dbView);
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

  /**
   * A fail-fast iterator.
   *
   * @param <E> type of elements returned by the iterator.
   */
  private class ConfigurableIter<E> extends RustIter<E> {

    private final Function<Long, E> nextFunction;
    private final Consumer<Long> disposeOperation;
    private final ViewModificationCounter modificationListener;
    private final Integer mapModCount;

    ConfigurableIter(long nativeHandle,
                     Function<Long, E> nextFunction,
                     Consumer<Long> disposeOperation,
                     ViewModificationCounter modificationListener) {
      super(nativeHandle, true);
      this.nextFunction = nextFunction;
      this.disposeOperation = disposeOperation;
      this.modificationListener = modificationListener;
      this.mapModCount = modificationListener.getModificationCount(dbView);
    }

    @Override
    public Optional<E> next() {
      checkNotModified();
      return Optional.ofNullable(nextFunction.apply(getNativeHandle()));
    }

    // todo(dt): Move to StoragePreconditions?
    private void checkNotModified() {
      if (modificationListener.isModifiedSince(dbView, mapModCount)) {
        throw new ConcurrentModificationException("Fork was modified during iteration: " + dbView);
      }
    }

    @Override
    void disposeInternal() {
      checkValid(MapIndexProxy.this);
      disposeOperation.accept(getNativeHandle());
    }
  }
}
