package com.exonum.binding.storage.indices;

import static com.google.common.base.Preconditions.checkNotNull;

import com.exonum.binding.proxy.ProxyContext;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.database.ViewProxy;
import com.exonum.binding.storage.serialization.Serializer;
import java.util.NoSuchElementException;


/**
 * An XEntry is a database index that can contain no or a single value.
 *
 * <p>An entry is analogous to {@link java.util.Optional}, but provides modifying ("destructive")
 * operations when created with a {@link Fork}.
 * Such methods are specified to throw {@link UnsupportedOperationException} if
 * the entry is created with a {@link Snapshot} — a read-only database view.
 *
 * <p>All method arguments are non-null by default.
 *
 * <p>This class is not thread-safe and and its instances shall not be shared between threads.
 *
 * @see View
 */
public class XEntry<T> {

  private final EntryIndexProxy delegate;
  private final Serializer<T> serializer;

  public static <T> XEntry<T> newInstance(String name, View view, Serializer<T> serializer) {
    ViewProxy viewProxy = view.getProxy();
    checkNotNull(serializer);

    EntryIndexProxy entryProxy = EntryIndexProxy.newInstance(name, viewProxy);
    ProxyContext context = view.getContext();
    context.add(entryProxy);

    return new XEntry<>(entryProxy, serializer);
  }

  private XEntry(EntryIndexProxy delegate, Serializer<T> serializer) {
    this.delegate = delegate;
    this.serializer = serializer;
  }

  /**
   * Sets a new value of the entry, overwriting the previous value.
   *
   * @param value a value to set. Must not be null.
   * @throws UnsupportedOperationException if this entry is read-only
   * @throws IllegalStateException if the proxy is invalid
   */
  public void set(T value) {
    byte[] valueBytes = serializer.toBytes(value);
    delegate.set(valueBytes);
  }

  /**
   * Returns true if this entry exists in the database.
   *
   * @throws IllegalStateException if the proxy is invalid.
   */
  public boolean isPresent() {
    return delegate.isPresent();
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
    byte[] valueBytes = delegate.get();
    return serializer.fromBytes(valueBytes);
  }

  /**
   * Removes a value from this entry.
   *
   * @throws UnsupportedOperationException if the entry is read-only.
   * @throws IllegalStateException if the proxy is invalid
   */
  public void remove() {
    delegate.remove();
  }
}
