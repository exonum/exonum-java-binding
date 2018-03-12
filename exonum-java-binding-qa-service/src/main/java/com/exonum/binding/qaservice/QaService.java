package com.exonum.binding.qaservice;

import com.exonum.binding.service.AbstractService;
import com.exonum.binding.service.Node;
import com.exonum.binding.service.Schema;
import com.exonum.binding.service.TransactionConverter;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.View;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import java.util.Optional;

/**
 * A simple service for QA purposes.
 *
 * @implNote This service is meant to be used to test integration of Exonum Java Binding with
 *     Exonum Core. It contains very little business-logic so the QA team can focus
 *     on testing the integration <em>through</em> this service, not the service itself.
 *
 *     <p>Such service is not meant to be illustrative, it breaks multiple recommendations
 *     on implementing Exonum Services, therefore, it shall NOT be used as an example
 *     of a user service.
 */
public class QaService extends AbstractService {

  public static final short ID = 127;
  static final String NAME = "ejb-qa-service";

  @Inject
  public QaService(TransactionConverter transactionConverter) {
    super(ID, NAME, transactionConverter);
  }

  @Override
  protected Schema createDataSchema(View view) {
    return new QaSchema(view);
  }

  @Override
  public Optional<String> initialize(Fork fork) {
    // todo: next PR
    return Optional.empty();
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    // todo: next PR
  }
}
