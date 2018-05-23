package com.exonum.binding.fakes.services.service;

import com.exonum.binding.hash.HashCode;
import com.exonum.binding.service.Schema;
import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.indices.ProofMapIndexProxy;
import com.exonum.binding.storage.serialization.StandardSerializers;
import java.util.Collections;
import java.util.List;

public final class TestSchema implements Schema {
  @SuppressWarnings("WeakerAccess")
  static final String TEST_MAP_NAME = TestService.NAME + "_test_map";

  private final View view;

  public TestSchema(View view) {
    this.view = view;
  }

  public ProofMapIndexProxy<HashCode, String> testMap() {
    return ProofMapIndexProxy.newInstance(TEST_MAP_NAME, view, StandardSerializers.hash(),
        StandardSerializers.string());
  }

  @Override
  public List<HashCode> getStateHashes() {
    HashCode rootHash = testMap().getRootHash();
    return Collections.singletonList(rootHash);
  }
}
