package com.exonum.binding.service;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.exonum.binding.storage.database.Snapshot;
import com.exonum.binding.storage.database.View;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AbstractServiceTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void constructorDiscardsEmptyName() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    new ServiceUnderTest((short) 1, "", mock(TransactionConverter.class));
  }

  @Test
  public void constructorDiscardsNullName() throws Exception {
    expectedException.expect(NullPointerException.class);
    new ServiceUnderTest((short) 1, null, mock(TransactionConverter.class));
  }

  @Test
  public void constructorDiscardsNullConverter() throws Exception {
    expectedException.expect(NullPointerException.class);
    new ServiceUnderTest((short) 1, "service#1", null);
  }

  @Test
  public void getStateHashes_EmptySchema() throws Exception {
    Service service = new ServiceUnderTest((short) 1, "s1", mock(TransactionConverter.class));
    assertTrue(service.getStateHashes(mock(Snapshot.class)).isEmpty());
  }

  static class ServiceUnderTest extends AbstractService {

    ServiceUnderTest(short id, String name,
                     TransactionConverter transactionConverter) {
      super(id, name, transactionConverter);
    }

    @Override
    protected Schema createDataSchema(View view) {
      return mock(Schema.class);
    }

    @Override
    public void createPublicApiHandlers() {}

    @Override
    public void createPrivateApiHandlers() {}
  }
}
