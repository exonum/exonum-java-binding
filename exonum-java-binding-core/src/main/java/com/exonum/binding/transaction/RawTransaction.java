package com.exonum.binding.transaction;

import static com.exonum.binding.common.hash.Hashing.sha256;

import com.exonum.binding.common.hash.HashCode;
import com.google.common.base.Objects;

public class RawTransaction {
  private final short serviceId;
  private final short transactionId;
  private final byte[] payload;

  private RawTransaction(short serviceId, short transactionId, final byte[] payload) {
    this.serviceId = serviceId;
    this.transactionId = transactionId;
    this.payload = payload.clone();
  }

  public short getServiceId() {
    return serviceId;
  }

  public short getTransactionId() {
    return transactionId;
  }

  public byte[] getPayload() {
    return payload.clone();
  }

  /**
   * Returns the SHA-256 hash raw transaction payload.
   */
  public HashCode hash() {
    return sha256().hashBytes(getPayload());
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RawTransaction that = (RawTransaction) o;
    return serviceId == that.serviceId
        && transactionId == that.transactionId
        && Objects.equal(payload, that.payload);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(serviceId, transactionId, payload);
  }

  public static final class Builder {
    private Short serviceId;
    private Short transactionId;
    private byte[] payload;

    /**
     * Sets service identifier to the transaction message.
     */
    public RawTransaction.Builder serviceId(short serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    /**
     * Sets transaction identifier to the transaction message.
     */
    public RawTransaction.Builder transactionId(short transactionId) {
      this.transactionId = transactionId;
      return this;
    }

    /**
     * Sets payload to the transaction message.
     */
    public RawTransaction.Builder payload(byte[] payload) {
      this.payload = payload.clone();
      return this;
    }

    public RawTransaction build() {
      checkRequiredFieldsSet();

      return new RawTransaction(this.serviceId, this.transactionId, this.payload);
    }

    private void checkRequiredFieldsSet() {
      String undefinedFields = "";
      undefinedFields = this.serviceId == null ? undefinedFields + " serviceId" : undefinedFields;
      undefinedFields =
          this.transactionId == null ? undefinedFields + " transactionId" : undefinedFields;
      undefinedFields = this.payload == null ? undefinedFields + " payload" : undefinedFields;
      if (!undefinedFields.isEmpty()) {
        throw new IllegalStateException(
            "Following field(s) are required but weren't set: " + undefinedFields);
      }
    }

    private Builder() {
    }
  }
}
