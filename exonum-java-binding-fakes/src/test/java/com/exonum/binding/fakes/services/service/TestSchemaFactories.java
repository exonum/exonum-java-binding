package com.exonum.binding.fakes.services.service;

import com.exonum.binding.storage.database.Fork;

final class TestSchemaFactories {

  static <SchemaT> SchemaFactory<SchemaT> createTestSchemaFactory(Fork expectedView,
                                                                  SchemaT schema) {
    return (actualView) -> {
      if (actualView.equals(expectedView)) {
        return schema;
      }
      throw new AssertionError("Unexpected view: " + actualView + ", expected: " + expectedView);
    };
  }

  private TestSchemaFactories() {}
}
