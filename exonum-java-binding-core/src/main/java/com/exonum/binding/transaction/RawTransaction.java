package com.exonum.binding.transaction;

import com.exonum.binding.service.Service;
import com.google.auto.value.AutoValue;

/**
 * An Exonum raw transaction. It is mainly used for interaction with the Exonum core
 * as well as for transferring transactions between nodes within the network.
 */
@AutoValue
public abstract class RawTransaction {

  /**
   * Returns a service identifier which the transaction belongs to.
   * @see Service#getId()
   */
  public abstract short getServiceId();

  /**
   * Returns the type of this transaction within a service.
   * @see com.exonum.binding.common.message.TransactionMessage#getTransactionId
   */
  public abstract short getTransactionId();

  /**
   * Returns the transaction payload which contains actual transaction data.
   */
  public abstract byte[] getPayload();

  /**
   * Returns the new builder for the transaction.
   */
  public static RawTransaction.Builder newBuilder() {
    return new AutoValue_RawTransaction.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    /**
     * Sets identifier of the service this transaction belongs to.
     */
    public abstract Builder serviceId(short serviceId);

    /**
     * Sets the identifier of the transaction within a service.
     */
    public abstract Builder transactionId(short transactionId);

    /**
     * Sets the payload of the transaction.
     */
    public abstract Builder payload(byte[] payload);

    /**
     * Creates the RawTransaction instance with specified parameters.
     */
    public abstract RawTransaction build();
  }

}
