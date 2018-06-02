package com.exonum.binding.storage.indices;

import com.exonum.binding.storage.database.View;
import com.exonum.binding.storage.serialization.StandardSerializers;

public class ListIndexProxyGroupIntegrationTest extends BaseListIndexProxyGroupTestable {

  @Override
  ListIndex<String> createInGroup(byte[] id, View view) {
    return ListIndexProxy.newInGroupUnsafe("list_index_group_IT", id, view,
        StandardSerializers.string());
  }
}
