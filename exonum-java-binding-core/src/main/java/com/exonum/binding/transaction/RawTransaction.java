package com.exonum.binding.transaction;

public class RawTransaction {
    private short serviceId;
    private short transactionId;
    private byte[] payload;

    public RawTransaction(short serviceId, short transactionId, byte[] payload) {
        this.serviceId = serviceId;
        this.transactionId = transactionId;
        this.payload = payload;
    }

    public short getServiceId() {
        return serviceId;
    }

    public short getTransactionId() {
        return transactionId;
    }

    public byte[] getPayload() {
        return payload;
    }
}
