package com.exonum.binding.storage.indices;

import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.serialization.StandardSerializers;

public class ProofListIndexProxyGroupIntegrationTest
    extends BaseListIndexProxyGroupTestable {

  @Override
  ListIndex<String> createInGroup(byte[] id, View view) {
    return ProofListIndexProxy.newInGroupUnsafe("proof_list_group_IT", id, view,
        StandardSerializers.string());
  }
}
