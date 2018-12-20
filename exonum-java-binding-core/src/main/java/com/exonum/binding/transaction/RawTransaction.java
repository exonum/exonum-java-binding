package com.exonum.binding.transaction;

import com.google.auto.value.AutoValue;

/**
 * An Exonum raw transaction. It is mainly used for interaction with the Exonum core
 * as well as for transferring transactions between nodes within the network.
 */
@AutoValue
public abstract class RawTransaction {

  /**
   * Returns a service identifier which the transaction belongs to.
   */
  public abstract short getServiceId();

  /**
   * Returns the type of this transaction within a service.
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
     * Sets service identifier to the transaction.
     */
    public abstract Builder serviceId(short serviceId);

    /**
     * Sets transaction identifier to the transaction.
     */
    public abstract Builder transactionId(short transactionId);

    /**
     * Sets payload to the transaction.
     */
    public abstract Builder payload(byte[] payload);

    public abstract RawTransaction build();
  }

}
