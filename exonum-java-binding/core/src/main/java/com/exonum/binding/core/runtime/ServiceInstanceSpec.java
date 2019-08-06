package com.exonum.binding.core.runtime;

import com.exonum.binding.common.message.TransactionMessage;
import com.google.auto.value.AutoValue;

/**
 * A specification of a service instance.
 */
@AutoValue
public abstract class ServiceInstanceSpec {

  /**
   * Returns the name of the service instance. It serves as the primary identifier of this service
   * in most operations. It is assigned by the network administrators.
   */
  public abstract String getName();

  /**
   * Returns the numeric id of the service instance. Exonum assigns it to the service
   * on instantiation. It is mainly used to route the transaction messages belonging
   * to this instance.
   *
   * @see TransactionMessage#getServiceId()
   */
  public abstract int getId();

  /**
   * Returns the service artifact id.
   */
  public abstract ServiceArtifactId getArtifactId();

  public static ServiceInstanceSpec newInstance(String name, int id, ServiceArtifactId artifactId) {
    return new AutoValue_ServiceInstanceSpec(name, id, artifactId);
  }
}
