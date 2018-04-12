package com.exonum.binding.qaservice;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.hash.Hashing;
import com.exonum.binding.messages.Transaction;
import com.exonum.binding.qaservice.transactions.CreateCounterTx;
import com.exonum.binding.qaservice.transactions.IncrementCounterTx;
import com.exonum.binding.qaservice.transactions.InvalidThrowingTx;
import com.exonum.binding.qaservice.transactions.InvalidTx;
import com.exonum.binding.qaservice.transactions.UnknownTx;
import com.exonum.binding.qaservice.transactions.ValidThrowingTx;
import com.exonum.binding.service.Node;
import com.exonum.binding.service.NodeFake;
import com.exonum.binding.service.Schema;
import com.exonum.binding.service.TransactionConverter;
import com.exonum.binding.storage.database.Fork;
import com.exonum.binding.storage.database.MemoryDb;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.MapIndex;
import com.exonum.binding.util.LibraryLoader;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import java.util.Optional;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class QaServiceImplIntegrationTest {

  static {
    LibraryLoader.load();
  }

  @ClassRule
  public static RunTestOnContext vertxTestContextRule = new RunTestOnContext();

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  private QaServiceImpl service;
  private Node node;
  private Vertx vertx;


  @Before
  public void setUp() {
    TransactionConverter transactionConverter = mock(TransactionConverter.class);
    service = new QaServiceImpl(transactionConverter);
    node = mock(Node.class);
    vertx = vertxTestContextRule.vertx();
  }

  @Test
  public void createDataSchema() {
    View view = mock(View.class);
    Schema dataSchema = service.createDataSchema(view);

    assertThat(dataSchema).isInstanceOf(QaSchema.class);
  }

  @Test
  public void initialize() {
    try (MemoryDb db = new MemoryDb();
         Fork view = db.createFork()) {
      Optional<String> initialConfiguration = service.initialize(view);

      // Check the configuration.
      assertThat(initialConfiguration)
          .hasValue(QaServiceImpl.INITIAL_SERVICE_CONFIGURATION);

      // Check the changes made to the database.
      QaSchema schema = new QaSchema(view);
      try (MapIndex<HashCode, Long> counters = schema.counters();
           MapIndex<HashCode, String> counterNames = schema.counterNames()) {
        String counterName = "default";
        HashCode counterId = Hashing.sha256()
            .hashString(counterName, UTF_8);

        assertThat(counters.get(counterId)).isEqualTo(0L);
        assertThat(counterNames.get(counterId)).isEqualTo(counterName);
      }
    }
  }

  @Test
  public void submitCreateCounter() throws Exception {
    setServiceNode(node);

    String counterName = "bids";
    HashCode txHash = service.submitCreateCounter(counterName);

    Transaction expectedTx = new CreateCounterTx(counterName);

    assertThat(txHash).isEqualTo(expectedTx.hash());
    verify(node).submitTransaction(eq(expectedTx));
  }

  @Test
  public void submitIncrementCounter() throws Exception {
    setServiceNode(node);

    long seed = 1L;
    HashCode counterId = HashCode.fromInt(1);
    HashCode txHash = service.submitIncrementCounter(seed, counterId);

    Transaction expectedTx = new IncrementCounterTx(seed, counterId);

    assertThat(txHash).isEqualTo(expectedTx.hash());
    verify(node).submitTransaction(eq(expectedTx));
  }

  @Test
  public void submitInvalidTx() throws Exception {
    setServiceNode(node);

    service.submitInvalidTx();
    verify(node).submitTransaction(any(InvalidTx.class));
  }

  @Test
  public void submitInvalidThrowingTx() throws Exception {
    setServiceNode(node);

    service.submitInvalidThrowingTx();
    verify(node).submitTransaction(any(InvalidThrowingTx.class));
  }

  @Test
  public void submitValidThrowingTx() throws Exception {
    setServiceNode(node);

    long seed = 1L;
    HashCode txHash = service.submitValidThrowingTx(seed);

    Transaction expectedTx = new ValidThrowingTx(seed);

    assertThat(txHash).isEqualTo(expectedTx.hash());
    verify(node).submitTransaction(eq(expectedTx));
  }

  @Test
  public void submitUnknownTx() throws Exception {
    setServiceNode(node);
    
    HashCode txHash = service.submitUnknownTx();
    
    Transaction expectedTx = new UnknownTx();
    
    assertThat(txHash).isEqualTo(expectedTx.hash());
    verify(node).submitTransaction(any(UnknownTx.class));
  }

  @Test
  public void submitUnknownTxBeforeNodeIsSet() {
    // Do not set the node: try to submit transaction with a null node.

    expectedException.expect(IllegalStateException.class);
    service.submitUnknownTx();
  }

  @Test
  public void getValue() {
    try (MemoryDb db = new MemoryDb()) {
      node = new NodeFake(db);
      setServiceNode(node);

      // Create a counter with the given name
      String counterName = "bids";
      try (Fork view = db.createFork()) {
        new CreateCounterTx(counterName)
            .execute(view);

        db.merge(view);
      }

      // Check that the service returns expected value
      HashCode counterId = Hashing.sha256().hashString(counterName, UTF_8);
      Optional<Counter> counterValueOpt = service.getValue(counterId);
      Counter expectedCounter = new Counter(counterName, 0L);
      assertThat(counterValueOpt).hasValue(expectedCounter);
    }
  }

  @Test
  public void getValueNoSuchCounter() {
    try (MemoryDb db = new MemoryDb()) {
      node = new NodeFake(db);
      setServiceNode(node);

      HashCode counterId = Hashing.sha256().hashString("Unknown counter", UTF_8);
      // Check there is no such counter
      assertThat(service.getValue(counterId)).isEmpty();
    }
  }

  @Test
  public void getValueBeforeInit() {
    expectedException.expect(IllegalStateException.class);
    service.getValue(HashCode.fromInt(1));
  }

  private void setServiceNode(Node node) {
    Router router = Router.router(vertx);
    service.createPublicApiHandlers(node, router);
  }
}
