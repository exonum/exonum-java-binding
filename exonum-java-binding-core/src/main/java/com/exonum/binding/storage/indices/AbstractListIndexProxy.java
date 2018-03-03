package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.StoragePreconditions.checkElementIndex;
import static com.exonum.binding.storage.indices.StoragePreconditions.checkNoNulls;

import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.serialization.CheckingSerializerDecorator;
import com.exonum.binding.storage.serialization.Serializer;
import com.google.errorprone.annotations.MustBeClosed;
import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * An abstract class for list indices implementing {@link ListIndex} interface.
 *
 * <p>Implements all methods from ListIndex.
 */
abstract class AbstractListIndexProxy<T> extends AbstractIndexProxy implements ListIndex<T> {

  final CheckingSerializerDecorator<T> serializer;

  AbstractListIndexProxy(long nativeHandle, String name, View view, Serializer<T> userSerializer) {
    super(nativeHandle, name, view);
    this.serializer = CheckingSerializerDecorator.from(userSerializer);
  }

  @Override
  public final void add(T e) {
    notifyModified();
    byte[] dbElement = serializer.toBytes(e);
    nativeAdd(getNativeHandle(), dbElement);
  }

  @Override
  public void addAll(Collection<? extends T> elements) {
    notifyModified();
    checkNoNulls(elements);
    addAllUnchecked(elements);
  }

  private void addAllUnchecked(Collection<? extends T> elements) {
    // Cache the nativeHandle to avoid repeated 'isValid' checks.
    // It's OK to do that during this call, as this class is not thread-safe.
    long nativeHandle = getNativeHandle();
    elements.stream()
        .map(serializer::toBytes)
        .forEach((e) -> nativeAdd(nativeHandle, e));
  }

  @Override
  public final void set(long index, T e) {
    checkElementIndex(index, size());
    notifyModified();
    byte[] dbElement = serializer.toBytes(e);
    nativeSet(getNativeHandle(), index, dbElement);
  }

  @Override
  public final T get(long index) {
    checkElementIndex(index, size());
    byte[] e = nativeGet(getNativeHandle(), index);
    return serializer.fromBytes(e);
  }

  @Override
  public final T getLast() {
    byte[] e = nativeGetLast(getNativeHandle());
    // This method does not check if the list is empty first to use only a single native call.
    if (e == null) {
      throw new NoSuchElementException("List is empty");
    }
    return serializer.fromBytes(e);
  }

  @Override
  public final void clear() {
    notifyModified();
    nativeClear(getNativeHandle());
  }

  @Override
  public final boolean isEmpty() {
    return nativeIsEmpty(getNativeHandle());
  }

  @Override
  public final long size() {
    return nativeSize(getNativeHandle());
  }

  @Override
  @MustBeClosed
  public final StorageIterator<T> iterator() {
    return StorageIterators.createIterator(
        nativeCreateIter(getNativeHandle()),
        this::nativeIterNext,
        this::nativeIterFree,
        this,
        modCounter,
        serializer::fromBytes);
  }

  @Override
  protected final void disposeInternal() {
    nativeFree(getNativeHandle());
  }

  abstract void nativeFree(long nativeHandle);

  abstract void nativeAdd(long nativeHandle, byte[] e);

  abstract void nativeSet(long nativeHandle, long index, byte[] e);

  abstract byte[] nativeGet(long nativeHandle, long index);

  abstract byte[] nativeGetLast(long nativeHandle);

  abstract void nativeClear(long nativeHandle);

  abstract boolean nativeIsEmpty(long nativeHandle);

  abstract long nativeSize(long nativeHandle);

  abstract long nativeCreateIter(long nativeHandle);

  abstract byte[] nativeIterNext(long iterNativeHandle);

  abstract void nativeIterFree(long iterNativeHandle);
}
