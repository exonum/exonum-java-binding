package com.exonum.binding.cryptocurrency;

import static com.google.common.base.Preconditions.checkState;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.messages.InternalServerError;
import com.exonum.binding.messages.InvalidTransactionException;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.service.AbstractService;
import com.exonum.binding.service.Node;
import com.exonum.binding.service.Schema;
import com.exonum.binding.service.TransactionConverter;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.View;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import java.util.Optional;
import javax.annotation.Nullable;

/** A cryptocurrency demo service. */
public class CryptocurrencyServiceImpl extends AbstractService implements CryptocurrencyService {

  @Nullable private Node node;

  @Inject
  public CryptocurrencyServiceImpl(TransactionConverter transactionConverter) {
    super(ID, NAME, transactionConverter);
  }

  @Override
  protected Schema createDataSchema(View view) {
    return new CryptocurrencySchema(view);
  }

  @Override
  public Optional<String> initialize(Fork fork) {
    return Optional.empty();
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    this.node = node;

    ApiController controller = new ApiController(this);
    controller.mountApi(router);
  }

  @Override
  @SuppressWarnings("ConstantConditions")
  public HashCode submitTransaction(Transaction tx)
      throws InvalidTransactionException, InternalServerError {
    checkBlockchainInitialized();
    node.submitTransaction(tx);
    return tx.hash();
  }

  private void checkBlockchainInitialized() {
    checkState(node != null, "Service has not been fully initialized yet");
  }
}
