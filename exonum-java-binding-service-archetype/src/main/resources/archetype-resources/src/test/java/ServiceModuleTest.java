package ${groupId};

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import com.exonum.binding.service.Service;
import com.exonum.binding.service.TransactionConverter;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Test;

public class ServiceModuleTest {

  @Test
  public void testServiceBinding() {
    Injector injector = createInjector();

    Service s = injector.getInstance(Service.class);

    assertThat(s, instanceOf(MyService.class));
  }

  @Test
  public void testTransactionConverterBinding() {
    Injector injector = createInjector();

    TransactionConverter s = injector.getInstance(TransactionConverter.class);

    assertThat(s, instanceOf(MyTransactionConverter.class));
  }

  private Injector createInjector() {
    return Guice.createInjector(new ServiceModule());
  }
}
