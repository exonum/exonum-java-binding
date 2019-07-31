package com.exonum.binding.core.runtime;

import com.exonum.binding.common.message.TransactionMessage;
import com.exonum.binding.core.service.Node;
import com.google.auto.value.AutoValue;
import io.vertx.ext.web.Router;

/**
 * A specification of a service instance.
 */
@AutoValue
public abstract class ServiceInstanceSpec {

  /**
   * Returns the service artifact id.
   */
  public abstract ServiceArtifactId getArtifactId();

  /**
   * Returns the name of the service instance. It serves as the unique string identifier
   * of this service, e.g., to route the requests to its public API.
   *
   * @see com.exonum.binding.core.service.Service#createPublicApiHandlers(Node, Router)
   */
  public abstract String getName();

  /**
   * Returns the numeric id of the service instance. It is mainly used to route the transaction
   * messages belonging to this instance.
   *
   * @see TransactionMessage#getServiceId()
   */
  public abstract int getId();
}
