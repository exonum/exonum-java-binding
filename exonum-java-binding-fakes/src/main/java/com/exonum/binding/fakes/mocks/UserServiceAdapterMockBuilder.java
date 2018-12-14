/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.fakes.mocks;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
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

  public void convertTransaction(UserTransactionAdapter transaction) {
    when(service.convertTransaction(anyShort(), anyShort(), any(byte[].class)))
        .thenReturn(checkNotNull(transaction));
  }

  /**
   * Sets up the mock to reject any transaction message,
   * as if it is does not belong to this service.
   */
  public void convertTransactionThrowing(Class<? extends Throwable> exceptionType) {
    when(service.convertTransaction(anyShort(), anyShort(), any(byte[].class)))
        .thenThrow(exceptionType);
  }

  public void stateHashes(byte[][] stateHashes) {
    when(service.getStateHashes(anyLong()))
        .thenReturn(checkNotNull(stateHashes));
  }

  public void stateHashesThrowing(Class<? extends Throwable> exceptionType) {
    when(service.getStateHashes(anyLong()))
        .thenThrow(exceptionType);
  }

  public void initialGlobalConfig(String initialGlobalConfig) {
    when(service.initialize(anyLong()))
        .thenReturn(initialGlobalConfig);
  }

  public void initialGlobalConfigThrowing(Class<? extends Throwable> exceptionType) {
    when(service.initialize(anyLong()))
        .thenThrow(exceptionType);
  }

  public void afterCommitHandlerThrowing(Class<? extends Throwable> exceptionType) {
    doThrow(exceptionType)
        .when(service).afterCommit(anyLong(), anyInt(), anyLong());
  }

  /**
   * Creates the {@link MockInteraction} instance for testing the UserServiceAdapter#after_commit()
   * method.
   *
   * @return MockInteraction instance
   */
  public MockInteraction getMockInteractionAfterCommit() {
    String[] args = {"handle", "validator", "height"};
    MockInteraction interaction = new MockInteraction(args);
    doAnswer(interaction.createAnswer()).when(service).afterCommit(anyLong(), anyInt(), anyLong());
    return interaction;
  }

  public void mountPublicApiHandlerThrowing(Class<? extends Throwable> exceptionType) {
    doThrow(exceptionType)
        .when(service).mountPublicApiHandler(anyLong());
  }

  /** Returns a set-up mock. */
  public UserServiceAdapter build() {
    return service;
  }
}
