package com.exonum.binding.fakes.services.service;

import com.exonum.binding.storage.database.View;

/**
 * A factory of service schemas. Might be promoted to an ejb-core interface in future versions.
 */
@FunctionalInterface
public interface SchemaFactory<SchemaT> {

  SchemaT from(View view);
}
