/* 
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ${package};

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.exonum.binding.core.blockchain.Blockchain;
import com.exonum.binding.core.service.Service;
import com.exonum.binding.core.service.TransactionConverter;
import com.exonum.binding.testkit.TestKit;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;

class ServiceModuleTest {

  @Test
  void testServiceBinding() {
    Injector injector = createInjector();

    Service s = injector.getInstance(Service.class);

    assertThat(s, instanceOf(MyService.class));
  }

  @Test
  void testTransactionConverterBinding() {
    Injector injector = createInjector();

    TransactionConverter s = injector.getInstance(TransactionConverter.class);

    assertThat(s, instanceOf(MyTransactionConverter.class));
  }

  @Test
  void testKitTest() {
    try (TestKit testKit = TestKit.forService(ServiceModule.class)) {
      MyService service = testKit.getService(MyService.ID, MyService.class);

      // Check that genesis block was committed
      testKit.withSnapshot((view) -> {
        Blockchain blockchain = Blockchain.newInstance(view);
        assertThat(blockchain.getBlockHashes().size(), equalTo(1L));
      });
    }
  }

  private static Injector createInjector() {
    return Guice.createInjector(new ServiceModule());
  }
}
