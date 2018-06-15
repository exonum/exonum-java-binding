/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.messages.TemplateMessage;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.service.Service;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.transport.Server;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.impl.RouterImpl;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceAdapterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private Service service;

  @Mock
  private Server server;

  @Mock
  private ViewFactory viewFactory;

  @InjectMocks
  private UserServiceAdapter serviceAdapter;

  @Test
  public void convertTransaction_ThrowsIfNull() throws Exception {
    expectedException.expect(NullPointerException.class);
    serviceAdapter.convertTransaction(null);
  }

  @Test
  public void convertTransaction() throws Exception {
    short serviceId = (short) 0xA103;
    Transaction expectedTransaction = mock(Transaction.class);
    when(service.getId()).thenReturn(serviceId);
    when(service.convertToTransaction(any(BinaryMessage.class)))
        .thenReturn(expectedTransaction);

    byte[] message = getServiceMessage(serviceId)
        .getMessage()
        .array();

    UserTransactionAdapter transactionAdapter = serviceAdapter.convertTransaction(message);

    assertThat(transactionAdapter.transaction, equalTo(expectedTransaction));
  }

  @Test
  public void convertTransaction_InvalidServiceImplReturningNull() throws Exception {
    short serviceId = (short) 0xA103;
    when(service.getId()).thenReturn(serviceId);
    when(service.convertToTransaction(any(BinaryMessage.class)))
        // Such service impl. is not valid
        .thenReturn(null);

    byte[] message = getServiceMessage(serviceId)
        .getMessage()
        .array();

    expectedException.expectMessage("Invalid service implementation: "
        + "Service#convertToTransaction must never return null.");
    expectedException.expect(NullPointerException.class);
    serviceAdapter.convertTransaction(message);
  }

  /**
   * Returns some transaction of the given service.
   */
  private BinaryMessage getServiceMessage(short serviceId) {
    return new Message.Builder()
        .mergeFrom(TemplateMessage.TEMPLATE_MESSAGE)
        .setServiceId(serviceId)
        .buildRaw();
  }

  @Test
  public void getStateHashes_EmptyList() throws Exception {
    long snapshotHandle = 0x0A;
    Snapshot s = mock(Snapshot.class);
    when(viewFactory.createSnapshot(eq(snapshotHandle), any(Cleaner.class)))
        .thenReturn(s);

    when(service.getStateHashes(s))
        .thenReturn(emptyList());

    byte[][] hashes = serviceAdapter.getStateHashes(snapshotHandle);

    assertThat(hashes.length, equalTo(0));
  }

  @Test
  public void getStateHashes_SingletonList() throws Exception {
    long snapshotHandle = 0x0A;
    Snapshot s = mock(Snapshot.class);
    when(viewFactory.createSnapshot(eq(snapshotHandle), any(Cleaner.class)))
        .thenReturn(s);

    byte[] h1 = bytes("hash1");
    when(service.getStateHashes(s))
        .thenReturn(singletonList(HashCode.fromBytes(h1)));

    byte[][] hashes = serviceAdapter.getStateHashes(snapshotHandle);

    assertThat(hashes.length, equalTo(1));
    assertThat(hashes[0], equalTo(h1));
  }

  @Test
  public void getStateHashes_MultipleHashesList() throws Exception {
    long snapshotHandle = 0x0A;
    Snapshot s = mock(Snapshot.class);
    when(viewFactory.createSnapshot(eq(snapshotHandle), any(Cleaner.class)))
        .thenReturn(s);

    byte[][] hashes = {
        bytes("hash1"),
        bytes("hash2"),
        bytes("hash3")
    };
    List<HashCode> hashesFromService = Arrays.stream(hashes)
        .map(HashCode::fromBytes)
        .collect(Collectors.toList());

    when(service.getStateHashes(s))
        .thenReturn(hashesFromService);

    byte[][] actualHashes = serviceAdapter.getStateHashes(snapshotHandle);

    assertThat(actualHashes, equalTo(hashes));
  }

  @Test
  public void getStateHashes_ClosesCleaner() throws Exception {
    long snapshotHandle = 0x0A;
    byte[][] ignored = serviceAdapter.getStateHashes(snapshotHandle);

    ArgumentCaptor<Cleaner> ac = ArgumentCaptor.forClass(Cleaner.class);
    verify(viewFactory).createSnapshot(eq(snapshotHandle), ac.capture());

    Cleaner cleaner = ac.getValue();

    assertTrue(cleaner.isClosed());
  }

  @Test
  public void initialize_ClosesCleaner() throws Exception {
    long forkHandle = 0x0A;
    String ignored = serviceAdapter.initialize(forkHandle);

    ArgumentCaptor<Cleaner> ac = ArgumentCaptor.forClass(Cleaner.class);
    verify(viewFactory).createFork(eq(forkHandle), ac.capture());

    Cleaner cleaner = ac.getValue();

    assertTrue(cleaner.isClosed());
  }

  @Test
  public void mountPublicApiHandler() throws Exception {
    Router router = mock(RouterImpl.class);
    when(server.createRouter())
        .thenReturn(router);

    String serviceName = "service1";
    when(service.getName())
        .thenReturn(serviceName);

    serviceAdapter.mountPublicApiHandler(0x0A);
    verify(server).mountSubRouter(eq("/service1"), eq(router));
  }

  @Test
  public void mountPublicApiHandler_FailsOnSubsequentCalls() throws Exception {
    serviceAdapter.mountPublicApiHandler(0x0A);

    expectedException.expect(IllegalStateException.class);
    serviceAdapter.mountPublicApiHandler(0x0B);
  }
}
