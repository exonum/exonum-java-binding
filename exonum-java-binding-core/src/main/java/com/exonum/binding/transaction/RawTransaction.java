package com.exonum.binding.transaction;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.common.hash.HashFunction;
import com.exonum.binding.common.hash.Hashing;

public class RawTransaction {
  private final short serviceId;
  private final short transactionId;
  private final byte[] payload;

  public RawTransaction(short serviceId, short transactionId, final byte[] payload) {
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
    HashFunction hashFunction = Hashing.defaultHashFunction();
    return hashFunction.hashBytes(getPayload());
  }
}
