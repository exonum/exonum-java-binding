/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.blockchain.serialization;

import static com.exonum.binding.common.serialization.StandardSerializers.protobuf;
import static com.exonum.binding.common.transaction.TransactionResult.MAX_USER_DEFINED_ERROR_CODE;
import static com.exonum.binding.common.transaction.TransactionResult.SUCCESSFUL_RESULT_STATUS_CODE;
import static com.exonum.binding.common.transaction.TransactionResult.UNEXPECTED_ERROR_STATUS_CODE;

import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.transaction.TransactionResult;

public enum TransactionResultSerializer implements Serializer<TransactionResult> {
  INSTANCE;

  private static final Serializer<CoreProtos.TransactionResult> PROTO_SERIALIZER =
      protobuf(CoreProtos.TransactionResult.class);

  @Override
  public byte[] toBytes(TransactionResult value) {
    int status = convertToCoreStatusCode(value);
    CoreProtos.TransactionResult txLocation =
        CoreProtos.TransactionResult.newBuilder()
            .setStatus(status)
            .setDescription(value.getErrorDescription())
            .build();
    return txLocation.toByteArray();
  }

  @Override
  public TransactionResult fromBytes(byte[] binaryTransactionResult) {
    CoreProtos.TransactionResult copiedtxLocationProtos =
        PROTO_SERIALIZER.fromBytes(binaryTransactionResult);
    int status = copiedtxLocationProtos.getStatus();
    String description = copiedtxLocationProtos.getDescription();
    if (status <= MAX_USER_DEFINED_ERROR_CODE) {
      return TransactionResult.error(status, description);
    } else if (status == SUCCESSFUL_RESULT_STATUS_CODE) {
      return TransactionResult.successful();
    } else if (status == UNEXPECTED_ERROR_STATUS_CODE) {
      return TransactionResult.unexpectedError(description);
    } else {
      String message =
          String.format("Invalid status code: %s, must be in the range [0, 257]", status);
      throw new IllegalArgumentException(message);
    }
  }

  private int convertToCoreStatusCode(TransactionResult transactionResult) {
    switch (transactionResult.getType()) {
      case ERROR:
        return transactionResult.getErrorCode().getAsInt();
      case SUCCESS:
        return SUCCESSFUL_RESULT_STATUS_CODE;
      case UNEXPECTED_ERROR:
        return UNEXPECTED_ERROR_STATUS_CODE;
      default:
        throw new AssertionError("Unreachable: " + transactionResult);
    }
  }

}
