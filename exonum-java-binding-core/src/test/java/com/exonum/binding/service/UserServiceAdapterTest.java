package com.exonum.binding.service;

import static com.exonum.binding.test.Bytes.bytes;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.exonum.binding.messages.TemplateMessage;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.transport.Server;
import com.google.common.hash.HashCode;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.impl.RouterImpl;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    Server.class
})
public class UserServiceAdapterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Service service;

  private Server server;

  private UserServiceAdapter serviceAdapter;

  @Before
  public void setUp() throws Exception {
    service = mock(Service.class);
    server = mock(Server.class);
    serviceAdapter = new UserServiceAdapter(service, server);
  }

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
    when(service.getStateHashes(any(Snapshot.class)))
        .thenReturn(emptyList());

    byte[][] hashes = serviceAdapter.getStateHashes(0x0A);

    assertThat(hashes.length, equalTo(0));
  }

  @Test
  public void getStateHashes_SingletonList() throws Exception {
    byte[] h1 = bytes("hash1");
    when(service.getStateHashes(any(Snapshot.class)))
        .thenReturn(singletonList(HashCode.fromBytes(h1)));

    byte[][] hashes = serviceAdapter.getStateHashes(0x0A);

    assertThat(hashes.length, equalTo(1));
    assertThat(hashes[0], equalTo(h1));
  }

  @Test
  public void getStateHashes_MultipleHashesList() throws Exception {
    byte[][] hashes = {
        bytes("hash1"),
        bytes("hash2"),
        bytes("hash3")
    };
    List<HashCode> hashesFromService = Arrays.stream(hashes)
        .map(HashCode::fromBytes)
        .collect(Collectors.toList());

    when(service.getStateHashes(any(Snapshot.class)))
        .thenReturn(hashesFromService);

    byte[][] actualHashes = serviceAdapter.getStateHashes(0x0A);

    assertThat(actualHashes, equalTo(hashes));
  }

  @Test
  public void getStateHashes_ClosesSnapshot() throws Exception {
    long snapshotHandle = 0x0A;
    byte[][] hashes = serviceAdapter.getStateHashes(snapshotHandle);

    ArgumentCaptor<Snapshot> ac = ArgumentCaptor.forClass(Snapshot.class);
    verify(service).getStateHashes(ac.capture());

    Snapshot snapshot = ac.getValue();

    // Try to use a snapshot after the method has returned: it must be closed.
    expectedException.expect(IllegalStateException.class);
    snapshot.getViewNativeHandle();
  }

  @Test
  public void initalize_ClosesFork() throws Exception {
    long forkHandle = 0x0A;
    String ignored = serviceAdapter.initalize(forkHandle);

    ArgumentCaptor<Fork> ac = ArgumentCaptor.forClass(Fork.class);
    verify(service).initialize(ac.capture());

    Fork fork = ac.getValue();

    // Try to use the fork after the method has returned: it must be closed.
    expectedException.expect(IllegalStateException.class);
    fork.getViewNativeHandle();
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
    verify(server).mountSubRouter(eq(serviceName), eq(router));
  }

  @Test
  public void mountPublicApiHandler_FailsOnSubsequentCalls() throws Exception {
    serviceAdapter.mountPublicApiHandler(0x0A);

    expectedException.expect(IllegalStateException.class);
    serviceAdapter.mountPublicApiHandler(0x0B);
  }
}
