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

package com.exonum.binding.service.adapters;

import static com.exonum.binding.test.Bytes.bytes;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.collection.IsArrayWithSize.emptyArray;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.service.BlockCommittedEvent;
import com.exonum.binding.service.Service;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.test.Bytes;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.Transaction;
import com.exonum.binding.transport.Server;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.impl.RouterImpl;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceAdapterTest {

  @Mock
  private Service service;

  @Mock
  private Server server;

  @Mock
  private ViewFactory viewFactory;

  @Mock
  private Snapshot snapshot;

  @InjectMocks
  private UserServiceAdapter serviceAdapter;

  private static final short SERVICE_ID = (short) 0xA103;
  private static final short TRANSACTION_ID = 1;
  private static final long SNAPSHOT_HANDLE = 0x0A;
  private static final long HEIGHT = 1;
  private static final int VALIDATOR_ID = 1;

  @Test
  void convertTransaction_ThrowsIfNull() {
    assertThrows(NullPointerException.class, () ->
        serviceAdapter.convertTransaction(TRANSACTION_ID, null));
  }

  @Test
  void convertTransaction() {
    Transaction expectedTransaction = mock(Transaction.class);
    when(service.convertToTransaction(any(RawTransaction.class)))
        .thenReturn(expectedTransaction);
    when(service.getId()).thenReturn(SERVICE_ID);

    byte[] payload = Bytes.bytes(0x00, 0x01);

    UserTransactionAdapter transactionAdapter =
        serviceAdapter.convertTransaction(TRANSACTION_ID, payload);

    RawTransaction expectedRawTransaction = RawTransaction.newBuilder()
        .serviceId(SERVICE_ID)
        .transactionId(TRANSACTION_ID)
        .payload(payload)
        .build();

    assertThat(transactionAdapter.transaction, equalTo(expectedTransaction));
    verify(service).convertToTransaction(expectedRawTransaction);
  }

  @Test
  void convertTransaction_InvalidServiceImplReturningNull() {
    when(service.convertToTransaction(any(RawTransaction.class)))
        // Such service impl. is not valid
        .thenReturn(null);

    byte[] payload = Bytes.bytes(0x00, 0x01);

    NullPointerException thrown = assertThrows(NullPointerException.class,
        () -> serviceAdapter.convertTransaction(TRANSACTION_ID, payload));
    assertThat(thrown.getLocalizedMessage(), containsString("Invalid service implementation: "
        + "Service#convertToTransaction must never return null."));
  }

  @Test
  void getStateHashes_EmptyList() {
    when(viewFactory.createSnapshot(eq(SNAPSHOT_HANDLE), any(Cleaner.class)))
        .thenReturn(snapshot);

    when(service.getStateHashes(snapshot))
        .thenReturn(emptyList());

    byte[][] hashes = serviceAdapter.getStateHashes(SNAPSHOT_HANDLE);

    assertThat(hashes, emptyArray());
  }

  @Test
  void getStateHashes_SingletonList() {
    when(viewFactory.createSnapshot(eq(SNAPSHOT_HANDLE), any(Cleaner.class)))
        .thenReturn(snapshot);

    byte[] h1 = bytes("hash1");
    when(service.getStateHashes(snapshot))
        .thenReturn(singletonList(HashCode.fromBytes(h1)));

    byte[][] hashes = serviceAdapter.getStateHashes(SNAPSHOT_HANDLE);

    assertThat(hashes, arrayWithSize(1));
    assertThat(hashes[0], equalTo(h1));
  }

  @Test
  void getStateHashes_MultipleHashesList() {
    when(viewFactory.createSnapshot(eq(SNAPSHOT_HANDLE), any(Cleaner.class)))
        .thenReturn(snapshot);

    byte[][] hashes = {
        bytes("hash1"),
        bytes("hash2"),
        bytes("hash3")
    };
    List<HashCode> hashesFromService = Arrays.stream(hashes)
        .map(HashCode::fromBytes)
        .collect(Collectors.toList());

    when(service.getStateHashes(snapshot))
        .thenReturn(hashesFromService);

    byte[][] actualHashes = serviceAdapter.getStateHashes(SNAPSHOT_HANDLE);

    assertThat(actualHashes, equalTo(hashes));
  }

  @Test
  void getStateHashes_ClosesCleaner() {
    serviceAdapter.getStateHashes(SNAPSHOT_HANDLE);

    ArgumentCaptor<Cleaner> ac = ArgumentCaptor.forClass(Cleaner.class);
    verify(viewFactory).createSnapshot(eq(SNAPSHOT_HANDLE), ac.capture());

    Cleaner cleaner = ac.getValue();

    assertTrue(cleaner.isClosed());
  }

  @Test
  void initialize_ClosesCleaner() {
    long forkHandle = 0x0A;
    serviceAdapter.initialize(forkHandle);

    ArgumentCaptor<Cleaner> ac = ArgumentCaptor.forClass(Cleaner.class);
    verify(viewFactory).createFork(eq(forkHandle), ac.capture());

    Cleaner cleaner = ac.getValue();

    assertTrue(cleaner.isClosed());
  }

  @Test
  void mountPublicApiHandler() {
    Router router = mock(RouterImpl.class);
    when(server.createRouter())
        .thenReturn(router);

    String serviceName = "service1";
    when(service.getName())
        .thenReturn(serviceName);

    serviceAdapter.mountPublicApiHandler(0x0A);
    verify(server).mountSubRouter(eq("/api/service1"), eq(router));
  }

  @Test
  void mountPublicApiHandler_FailsOnSubsequentCalls() {
    serviceAdapter.mountPublicApiHandler(0x0A);

    assertThrows(IllegalStateException.class, () -> serviceAdapter.mountPublicApiHandler(0x0B));
  }

  @Test
  void afterCommit_ValidatorNode() {
    when(viewFactory.createSnapshot(eq(SNAPSHOT_HANDLE), any(Cleaner.class)))
        .thenReturn(snapshot);
    serviceAdapter.afterCommit(SNAPSHOT_HANDLE, VALIDATOR_ID, HEIGHT);

    ArgumentCaptor<BlockCommittedEvent> ac = ArgumentCaptor.forClass(BlockCommittedEvent.class);
    verify(service).afterCommit(ac.capture());

    BlockCommittedEvent event = ac.getValue();

    assertThat(event.getHeight(), equalTo(HEIGHT));
    assertThat(event.getValidatorId(), equalTo(OptionalInt.of(VALIDATOR_ID)));
    assertThat(event.getSnapshot(), equalTo(snapshot));
  }

  @Test
  void afterCommit_AuditorNode() {
    // For auditor nodes (which do not have validatorId) negative validatorId is passed
    int validatorId = -1;
    when(viewFactory.createSnapshot(eq(SNAPSHOT_HANDLE), any(Cleaner.class)))
        .thenReturn(snapshot);
    serviceAdapter.afterCommit(SNAPSHOT_HANDLE, validatorId, HEIGHT);

    ArgumentCaptor<BlockCommittedEvent> ac = ArgumentCaptor.forClass(BlockCommittedEvent.class);
    verify(service).afterCommit(ac.capture());

    BlockCommittedEvent event = ac.getValue();

    assertThat(event.getHeight(), equalTo(HEIGHT));
    assertThat(event.getValidatorId(), equalTo(OptionalInt.empty()));
  }

  @Test
  void afterCommit_ClosesCleaner() {
    when(viewFactory.createSnapshot(eq(SNAPSHOT_HANDLE), any(Cleaner.class)))
        .thenReturn(snapshot);
    serviceAdapter.afterCommit(SNAPSHOT_HANDLE, VALIDATOR_ID, HEIGHT);

    ArgumentCaptor<Cleaner> ac = ArgumentCaptor.forClass(Cleaner.class);
    verify(viewFactory).createSnapshot(eq(SNAPSHOT_HANDLE), ac.capture());

    Cleaner cleaner = ac.getValue();

    assertTrue(cleaner.isClosed());
  }

  @Test
  void afterCommit_swallowUnexpectedException() {
    when(viewFactory.createSnapshot(eq(SNAPSHOT_HANDLE), any(Cleaner.class))).thenReturn(snapshot);
    doThrow(NullPointerException.class).when(service).afterCommit(any(BlockCommittedEvent.class));

    serviceAdapter.afterCommit(SNAPSHOT_HANDLE, VALIDATOR_ID, HEIGHT);

    verify(service).afterCommit(any(BlockCommittedEvent.class));
  }

}
