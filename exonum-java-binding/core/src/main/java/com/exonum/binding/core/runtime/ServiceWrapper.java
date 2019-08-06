package com.exonum.binding.core.runtime;

import com.exonum.binding.core.service.Service;
import com.exonum.binding.core.storage.database.Fork;
import com.google.common.annotations.VisibleForTesting;
import java.util.Properties;

/**
 * The service wrapper represents an Exonum service as a whole and allows the service runtime
 * to operate on them conveniently. It separates the <em>extension</em>,
 * user-facing, interface from the <em>runtime</em>, internal, interface.
 */
final class ServiceWrapper {

  private final Service service;
  private final ServiceInstanceSpec instanceSpec;

  ServiceWrapper(Service service, ServiceInstanceSpec instanceSpec) {
    this.service = service;
    this.instanceSpec = instanceSpec;
  }

  String getName() {
    return instanceSpec.getName();
  }

  void configure(Fork view, Properties configuration) {
    service.configure(view, configuration);
  }

  @VisibleForTesting
  Service getService() {
    return service;
  }
}
