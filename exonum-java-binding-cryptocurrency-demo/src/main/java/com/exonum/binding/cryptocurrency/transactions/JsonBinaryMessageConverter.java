package com.exonum.binding.cryptocurrency.transactions;

import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.crypto.PublicKey;
import com.exonum.binding.cryptocurrency.CryptocurrencyService;
import com.exonum.binding.messages.BinaryMessage;
import com.exonum.binding.messages.Message;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.ByteString;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;

/**
 * A class converting JSON messages into binary messages.
 * Supports the transactions defined in this service.
 */
public final class JsonBinaryMessageConverter {

  private static final Gson GSON = CryptocurrencyTransactionGson.instance();
  private static final BaseEncoding HEX_ENCODING = BaseEncoding.base16().lowerCase();

  public JsonBinaryMessageConverter() {}

  /**
   * Converts a transaction message in JSON to a transaction message in binary format.
   *
   * @param messageJson a transaction message in JSON format
   * @return a binary message, corresponding to this transaction
   * @throws IllegalArgumentException if the message is not correct (unknown, malformed,
   *     containing illegal data)
   * @see TransactionJsonMessage
   */
  public BinaryMessage toMessage(String messageJson) {
    TransactionJsonMessage rawMessage = GSON.fromJson(messageJson, TransactionJsonMessage.class);
    short serviceId = rawMessage.getServiceId();
    checkArgument(serviceId == CryptocurrencyService.ID,
        "Service id (%s) in the message (%s) does not belong to this service (%s)",
        serviceId, messageJson, CryptocurrencyService.ID);

    short messageId = rawMessage.getMessageId();
    switch (messageId) {
      case CreateWalletTx.ID: {
        Type messageType = new TypeToken<TransactionJsonMessage<CreateWalletTxData>>() {}.getType();
        TransactionJsonMessage<CreateWalletTxData> message =
            GSON.fromJson(messageJson, messageType);

        CreateWalletTxData txParameters = message.getBody();
        byte[] binaryBody = TxMessagesProtos.CreateWalletTx.newBuilder()
            .setOwnerPublicKey(publicKeyToProtoBytes(txParameters.ownerPublicKey))
            .setInitialBalance(txParameters.initialBalance)
            .build()
            .toByteArray();

        return toBinaryMessage(message, binaryBody);
      }

      case TransferTx.ID: {
        Type messageType = new TypeToken<TransactionJsonMessage<TransferTxData>>() {}.getType();
        TransactionJsonMessage<TransferTxData> message = GSON.fromJson(messageJson, messageType);

        TransferTxData txParameters = message.getBody();
        byte[] binaryBody = TxMessagesProtos.TransferTx.newBuilder()
            .setSeed(txParameters.seed)
            .setFromWallet(publicKeyToProtoBytes(txParameters.senderId))
            .setToWallet(publicKeyToProtoBytes(txParameters.recipientId))
            .setSum(txParameters.amount)
            .build()
            .toByteArray();

        return toBinaryMessage(message, binaryBody);
      }

      default: throw new IllegalArgumentException("Unknown message id (" + messageId + "): "
          + messageJson);
    }
  }

  private static BinaryMessage toBinaryMessage(TransactionJsonMessage<?> message,
                                               byte[] binaryBody) {
    return new Message.Builder()
        .setMessageType(message.getMessageId())
        .setServiceId(message.getServiceId())
        .setVersion(message.getProtocolVersion())
        .setBody(ByteBuffer.wrap(binaryBody))
        .setSignature(ByteBuffer.wrap(decodeHex(message.getSignature())))
        .buildRaw();
  }

  private static ByteString publicKeyToProtoBytes(
      PublicKey publicKey) {
    return ByteString.copyFrom(publicKey.toBytes());
  }

  private static byte[] decodeHex(String s) {
    return HEX_ENCODING.decode(s);
  }
}
