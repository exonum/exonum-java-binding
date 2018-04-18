package com.exonum.binding.fakes.services.service;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.service.AbstractService;
import com.exonum.binding.service.Node;
import com.exonum.binding.storage.database.ForkProxy;
import com.exonum.binding.storage.database.ViewProxy;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import io.vertx.ext.web.Router;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

// Used in native code, see exonum-java-binding-core/rust/integration_tests
@SuppressWarnings("WeakerAccess")
public final class TestService extends AbstractService {

  public static final short ID = 0x110B;
  public static final String NAME = "experimentalTestService";

  static final String INITIAL_CONFIGURATION = "{ \"version\": 0.1 }";
  static final HashCode INITIAL_ENTRY_KEY = Hashing.defaultHashFunction()
      .hashString("initial key", StandardCharsets.UTF_8);
  static final String INITIAL_ENTRY_VALUE = "initial value";

  private final SchemaFactory<TestSchema> schemaFactory;

  public TestService(SchemaFactory<TestSchema> schemaFactory) {
    super(ID, NAME, (rawTx) -> PutValueTransaction.from(rawTx, schemaFactory));
    this.schemaFactory = schemaFactory;
  }

  @Override
  protected TestSchema createDataSchema(ViewProxy view) {
    return schemaFactory.from(view);
  }

  /**
   * Always puts the same value identified by the same key and returns the same configuration.
   */
  @Override
  public Optional<String> initialize(ForkProxy fork) {
    TestSchema schema = createDataSchema(fork);
    try (ProofMapIndexProxy<HashCode, String> testMap = schema.testMap()) {
      testMap.put(INITIAL_ENTRY_KEY, INITIAL_ENTRY_VALUE);
      return Optional.of(INITIAL_CONFIGURATION);
    }
  }

  @Override
  public void createPublicApiHandlers(Node node, Router router) {
    // No-op: no handlers.
  }
}
