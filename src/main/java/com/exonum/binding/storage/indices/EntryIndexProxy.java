package com.exonum.binding.storage.indices;

import static com.exonum.binding.proxy.ProxyPreconditions.checkValid;
import static com.exonum.binding.storage.indices.StoragePreconditions.checkIndexPrefix;
import static com.exonum.binding.storage.indices.StoragePreconditions.checkStorageValue;

import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.database.View;
import java.util.NoSuchElementException;

/**
 * An Entry is a database index that can contain no or a single value.
 *
 * <p>An Entry is analogous to {@link java.util.Optional}, but provides modifying ("destructive")
 * operations when created with a {@link Fork}.
 * Such methods are specified to throw {@link UnsupportedOperationException} if
 * the entry is created with a {@link Snapshot} — a read-only database view.
 *
 * <p>As any native proxy, the entry <em>must be closed</em> when no longer needed.
 * Subsequent use of the closed entry is prohibited
 * and will result in {@link IllegalStateException}.
 */
public class EntryIndexProxy extends AbstractIndexProxy {

  /**
   * Creates a new Entry.
   *
   * @param prefix a unique identifier of the Entry in the underlying storage
   * @param view a database view. Must be valid.
   *             If a view is read-only, "destructive" operations are not permitted.
   *
   * @throws NullPointerException if any argument is null
   * @throws IllegalArgumentException if the prefix is empty
   * @throws IllegalStateException if the view proxy is invalid
   */
  EntryIndexProxy(byte[] prefix, View view) {
    super(nativeCreate(checkIndexPrefix(prefix), view.getViewNativeHandle()), view);
  }

  /**
   * Sets a new value of the entry, overwriting the previous value.
   *
   * @param value a value to set. Must not be null.
   * @throws UnsupportedOperationException if the entry is read-only
   * @throws NullPointerException if value is null
   * @throws IllegalStateException if the proxy is invalid
   */
  public void set(byte[] value) {
    notifyModified();
    nativeSet(getNativeHandle(), checkStorageValue(value));
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
   */
  public byte[] get() {
    byte[] value = nativeGet(getNativeHandle());
    if (value == null) {
      throw new NoSuchElementException("No value in this entry");
    }
    return value;
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

  @Override
  protected void disposeInternal() {
    checkValid(dbView);
    nativeFree(getNativeHandle());
  }

  private static native long nativeCreate(byte[] prefix, long viewNativeHandle);

  private native void nativeSet(long nativeHandle, byte[] value);

  private native boolean nativeIsPresent(long nativeHandle);

  private native byte[] nativeGet(long nativeHandle);

  @SuppressWarnings("unused")
  private native byte[] nativeGetHash(long nativeHandle);

  private native void nativeRemove(long nativeHandle);

  private native void nativeFree(long nativeHandle);
}
