package com.exonum.binding.service.adapters;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.proxy.Cleaner;
import com.exonum.binding.proxy.CloseFailuresException;
import com.exonum.binding.service.NodeProxy;
import com.exonum.binding.service.Service;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.transport.Server;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import java.util.List;
import javax.annotation.Nullable;

/**
 * An adapter of a user-facing interface {@link Service} to an interface with a native code.
 */
@SuppressWarnings({"unused", "WeakerAccess"})  // Methods are called from the native proxy
public final class UserServiceAdapter {

  private final Service service;

  private final Server server;

  @Nullable
  private NodeProxy node;

  @Inject
  public UserServiceAdapter(Service service, Server server) {
    this.service = checkNotNull(service, "service");
    this.server = checkNotNull(server, "server");
  }

  public short getId() {
    return service.getId();
  }

  public String getName() {
    return service.getName();
  }

  /**
   * Converts a transaction messages into an executable transaction of this service.
   *
   * <p>The callee must handle the declared exceptions.
   *
   * @param transactionMessage a transaction message to be converted
   * @return an executable transaction of this service
   *         todo: exception(-s) is to be revised when we (a) design the native part and
   *         (b) implement a certain serialization format
   * @throws NullPointerException if transactionMessage is null, or a user service returns
   *     a null transaction
   * @throws IllegalArgumentException if message is not a valid transaction message of this service
   */
  public UserTransactionAdapter convertTransaction(byte[] transactionMessage) {
    BinaryMessage message = BinaryMessage.fromBytes(transactionMessage);
    assert message.getServiceId() == getId() :
        "Message id is distinct from the service id";

    Transaction transaction = service.convertToTransaction(message);
    checkNotNull(transaction, "Invalid service implementation: "
            + "Service#convertToTransaction must never return null.\n"
            + "Throw an exception if your service does not recognize this message id (%s)",
        message.getMessageType());  // todo: consider moving this check to the native code?
    return new UserTransactionAdapter(transaction);
  }

  /**
   * Returns the state hashes of the service.
   *
   * <p>The method does not destroy a native snapshot object corresponding to the passed handle.</p>
   *
   * @param snapshotHandle a handle to a native snapshot object
   * @return an array of state hashes
   * @see Service#getStateHashes(Snapshot)
   */
  // todo: if the native code is better of with a flattened array, change the signature
  public byte[][] getStateHashes(long snapshotHandle) {
    assert snapshotHandle != 0;

    try (Cleaner cleaner = new Cleaner("UserServiceAdapter#getStateHashes")) {
      Snapshot snapshot = Snapshot.newInstance(snapshotHandle, false, cleaner);
      List<HashCode> stateHashes = service.getStateHashes(snapshot);
      return stateHashes.stream()
          .map(HashCode::asBytes)
          .toArray(byte[][]::new);
    } catch (CloseFailuresException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the service initial global configuration.
   *
   * <p>The method does not destroy a native fork object corresponding to the passed handle.</p>
   *
   * @param forkHandle a handle to a native fork object
   * @return the service global configuration as a JSON string or null if it does not have any
   * @see Service#initialize(Fork)
   */
  public String initialize(long forkHandle) {
    assert forkHandle != 0;
    try (Cleaner cleaner = new Cleaner("UserServiceAdapter#initialize")) {
      Fork fork = Fork.newInstance(forkHandle, false, cleaner);
      return service.initialize(fork)
          .orElse(null);
    } catch (CloseFailuresException e) {
      throw new RuntimeException(e);
    }
  }

  public void mountPublicApiHandler(long nodeNativeHandle) {
    checkState(node == null, "There is a node already: are you calling this method twice?");
    node = new NodeProxy(nodeNativeHandle);
    Router router = server.createRouter();
    service.createPublicApiHandlers(node, router);
    server.mountSubRouter(getName(), router);
  }

  /**
   * Close this service adapter.
   *
   * <p>Releases any resources.
   */
  public void close() {
    if (node != null) {
      node.close();
    }
  }
}
