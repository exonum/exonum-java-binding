package ${groupId};

import com.exonum.binding.service.AbstractService;
import com.exonum.binding.service.Node;
import com.exonum.binding.service.Schema;
import com.exonum.binding.service.TransactionConverter;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.View;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;

import java.util.Optional;

public final class MyService extends AbstractService {

  public static final short ID = 42;
  static final String NAME = "${serviceName}";
  static final String INITIAL_SERVICE_CONFIGURATION = "{ \"version\": 0.1 }";

  @Inject
  public MyService(TransactionConverter transactionConverter) {
    super(ID, NAME, transactionConverter);
  }

  @Override
  protected Schema createDataSchema(View view) {
    return new MySchema(view);
  }

  @Override
  public Optional<String> initialize(Fork fork) {
    return Optional.of(INITIAL_SERVICE_CONFIGURATION);
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
  }
}
