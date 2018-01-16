package com.exonum.binding.fakes.services;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.exonum.binding.service.adapters.UserServiceAdapter;
import com.exonum.binding.service.adapters.UserTransactionAdapter;

/**
 * A {@link UserServiceAdapter} mock builder.
 *
 * <p>You do not have to mock <em>any</em> methods: default values will be provided.
 */
@SuppressWarnings({"unused", "WeakerAccess"}) // Used in native code
public final class UserServiceAdapterMockBuilder {

  private final UserServiceAdapter service = mock(UserServiceAdapter.class);

  // The builder methods below return «void» as the native code can't enjoy chaining.
  public void id(short id) {
    when(service.getId()).thenReturn(id);
  }

  public void name(String name) {
    when(service.getName()).thenReturn(name);
  }

  public void convertingTo(UserTransactionAdapter transaction) {
    when(service.convertTransaction(any(byte[].class)))
        .thenReturn(checkNotNull(transaction));
  }

  public void stateHashes(byte[][] stateHashes) {
    when(service.getStateHashes(anyLong()))
        .thenReturn(checkNotNull(stateHashes));
  }

  public void initialGlobalConfig(String initialGlobalConfig) {
    when(service.initalize(anyLong()))
        .thenReturn(checkNotNull(initialGlobalConfig));
  }

  /** Returns a set-up mock. */
  public UserServiceAdapter build() {
    return service;
  }
}


