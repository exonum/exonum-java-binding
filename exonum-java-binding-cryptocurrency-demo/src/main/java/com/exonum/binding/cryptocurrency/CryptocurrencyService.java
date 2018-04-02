package com.exonum.binding.cryptocurrency;

import com.exonum.binding.service.AbstractService;
import com.exonum.binding.service.Node;
import com.exonum.binding.service.Schema;
import com.exonum.binding.service.TransactionConverter;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.View;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import java.util.Optional;

/** A cryptocurrency demo service. */
public class CryptocurrencyService extends AbstractService {

  public static final short ID = 42;
  static final String NAME = "cryptocurrency-demo-service";

  @Inject
  public CryptocurrencyService(TransactionConverter transactionConverter) {
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
    ApiController controller = new ApiController(node);
    controller.mountApi(router);
  }
}
