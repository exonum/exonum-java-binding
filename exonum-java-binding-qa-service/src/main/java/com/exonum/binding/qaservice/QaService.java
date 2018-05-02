package com.exonum.binding.qaservice;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.service.Service;
import java.util.Optional;

/**
 * A simple service for QA purposes.
 */
public interface QaService extends Service {

  short ID = 127;
  String NAME = "ejb-qa-service";

  HashCode submitCreateCounter(String counterName);

  HashCode submitIncrementCounter(long requestSeed, HashCode counterId);

  HashCode submitInvalidTx();

  HashCode submitInvalidThrowingTx();

  HashCode submitValidThrowingTx(long requestSeed);

  HashCode submitUnknownTx();

  Optional<Counter> getValue(HashCode counterId);
}
