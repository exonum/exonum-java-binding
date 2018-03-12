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
