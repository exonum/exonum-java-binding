package com.exonum.binding.storage.indices;

import static com.exonum.binding.storage.indices.StoragePreconditions.checkIndexName;

import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.NativeHandle;
import com.exonum.binding.proxy.ProxyDestructor;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.serialization.CheckingSerializerDecorator;
import com.exonum.binding.storage.serialization.Serializer;
import java.util.NoSuchElementException;

/**
 * An Entry is a database index that can contain no or a single value.
 *
 * <p>An Entry is analogous to {@link java.util.Optional}, but provides modifying ("destructive")
 * operations when created with a {@link Fork}.
 * Such methods are specified to throw {@link UnsupportedOperationException} if
 * the entry is created with a {@link Snapshot} — a read-only database view.
 *
 * <p>All method arguments are non-null by default.
 *
 * <p>This class is not thread-safe and and its instances shall not be shared between threads.
 *
 * <p>When the view goes out of scope, this entry is destroyed. Subsequent use of the closed entry
 * is prohibited and will result in {@link IllegalStateException}.
 *
 * @param <T> the type of an element in this entry
 *
 * @see View
 */
public final class EntryIndexProxy<T> extends AbstractIndexProxy {

  private final CheckingSerializerDecorator<T> serializer;

  /**
   * Creates a new Entry.
   *
   * @param name a unique alphanumeric non-empty identifier of the Entry in the underlying storage:
   *             [a-zA-Z0-9_]
   * @param view a database view. Must be valid.
   *             If a view is read-only, "destructive" operations are not permitted.
   * @param serializer an entry serializer
   *
   * @throws NullPointerException if any argument is null
   * @throws IllegalArgumentException if the name is empty
   * @throws IllegalStateException if the view proxy is invalid
   */
  public static <E> EntryIndexProxy<E> newInstance(
      String name, View view, Serializer<E> serializer) {
    checkIndexName(name);
    CheckingSerializerDecorator<E> s = CheckingSerializerDecorator.from(serializer);

    NativeHandle entryNativeHandle = createNativeEntry(name, view);

    return new EntryIndexProxy<>(entryNativeHandle, name, view, s);
  }

  private static NativeHandle createNativeEntry(String name, View view) {
    long viewNativeHandle = view.getViewNativeHandle();
    NativeHandle entryNativeHandle = new NativeHandle(nativeCreate(name, viewNativeHandle));

    Cleaner cleaner = view.getCleaner();
    ProxyDestructor.newRegistered(cleaner, entryNativeHandle, EntryIndexProxy.class,
        EntryIndexProxy::nativeFree);
    return entryNativeHandle;
  }

  private EntryIndexProxy(NativeHandle nativeHandle, String name, View view,
                          CheckingSerializerDecorator<T> serializer) {
    super(nativeHandle, name, view);
    this.serializer = serializer;
  }

  /**
   * Sets a new value of the entry, overwriting the previous value.
   *
   * @param value a value to set. Must not be null.
   * @throws UnsupportedOperationException if the entry is read-only
   * @throws NullPointerException if value is null
   * @throws IllegalStateException if the proxy is invalid
   */
  public void set(T value) {
    notifyModified();
    byte[] valueBytes = serializer.toBytes(value);
    nativeSet(getNativeHandle(), valueBytes);
  }

  /**
   * Returns true if this entry exists in the database.
   *
   * @throws IllegalStateException if the proxy is invalid.
   */
  public boolean isPresent() {
    return nativeIsPresent(getNativeHandle());
  }

  /**
   * If value is present in the entry, returns it, otherwise,
   * throws {@link NoSuchElementException}.
   *
   * @return a non-null value
   * @throws NoSuchElementException if a value is not present in the Entry
   * @throws IllegalStateException if the proxy is invalid
   * @throws IllegalArgumentException if the supplied serializer cannot decode the value
   */
  public T get() {
    byte[] value = nativeGet(getNativeHandle());
    if (value == null) {
      throw new NoSuchElementException("No value in this entry");
    }
    return serializer.fromBytes(value);
  }

  // TODO(dt): add getHash when you clarify why on Earth it returns a default (= zero) hash when
  // value is not present.

  /**
   * Removes a value from this entry.
   *
   * @throws UnsupportedOperationException if the entry is read-only.
   * @throws IllegalStateException if the proxy is invalid
   */
  public void remove() {
    notifyModified();
    nativeRemove(getNativeHandle());
  }

  private static native long nativeCreate(String name, long viewNativeHandle);

  private native void nativeSet(long nativeHandle, byte[] value);

  private native boolean nativeIsPresent(long nativeHandle);

  private native byte[] nativeGet(long nativeHandle);

  @SuppressWarnings("unused")
  private native byte[] nativeGetHash(long nativeHandle);

  private native void nativeRemove(long nativeHandle);

  private static native void nativeFree(long nativeHandle);
}
