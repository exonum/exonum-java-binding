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

package com.exonum.binding.qaservice.transactions;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;

public class QaTransactionTest {

  @Test
  public void hasUniqueIds() {
    QaTransaction[] elements = QaTransaction.values();

    Multimap<Short, QaTransaction> elementsById = Multimaps.index(Arrays.asList(elements),
        QaTransaction::id);

    for (Short id : elementsById.keySet()) {
      Collection<QaTransaction> transactionsWithId = elementsById.get(id);
      assertThat(transactionsWithId)
          .as("There must be a single transaction with id (%d), but: %s",
              id, transactionsWithId)
          .hasSize(1);
    }
  }
}
