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

package com.exonum.binding.qaservice.transactions;

import static com.exonum.binding.qaservice.transactions.TransactionPreconditions.checkTransaction;
import static com.google.common.base.Preconditions.checkArgument;

import com.exonum.binding.common.serialization.Serializer;
import com.exonum.binding.common.serialization.StandardSerializers;
import com.exonum.binding.qaservice.QaSchema;
import com.exonum.binding.qaservice.QaService;
import com.exonum.binding.qaservice.transactions.TxMessageProtos.ValidErrorTxBody;
import com.exonum.binding.transaction.RawTransaction;
import com.exonum.binding.transaction.Transaction;
import com.exonum.binding.transaction.TransactionContext;
import com.exonum.binding.transaction.TransactionExecutionException;
import com.google.common.base.Strings;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * A valid transaction that will always have "error" status, i.e.,
 * throw an {@link TransactionExecutionException}.
 * Clears all collections of this service before throwing the exception.
 */
public final class ValidErrorTx implements Transaction {

  private static final short ID = QaTransaction.VALID_ERROR.id();
  private static final Serializer<ValidErrorTxBody> PROTO_SERIALIZER =
      StandardSerializers.protobuf(ValidErrorTxBody.class);

  private final long seed;
  private final byte errorCode;
  @Nullable
  private final String errorDescription;

  /**
   * Creates a new transaction.
   *
   * @param seed a seed to distinguish transaction with the same parameters
   * @param errorCode an error code to include in the exception, must be in range [0; 127]
   * @param errorDescription an optional description to include in the exception,
   *     must be either null or non-empty
   * @throws IllegalArgumentException if the error code is not in range [0; 127]
   *     or error description is empty
   */
  public ValidErrorTx(long seed, byte errorCode, @Nullable String errorDescription) {
    // Reject negative errorCodes so that there is no confusion between *signed* Java byte
    // and *unsigned* errorCode that Rust persists.
    checkArgument(errorCode >= 0, "error code (%s) must be in range [0; 127]", errorCode);
    checkArgument(nullOrNonEmpty(errorDescription));
    this.seed = seed;
    this.errorCode = errorCode;
    this.errorDescription = errorDescription;
  }

  private static boolean nullOrNonEmpty(@Nullable String errorDescription) {
    return errorDescription == null || !errorDescription.isEmpty();
  }

  @Override
  public void execute(TransactionContext context) throws TransactionExecutionException {
    QaSchema schema = new QaSchema(context.getFork());

    // Attempt to clear all service indices.
    schema.clearAll();

    // Throw an exception. Framework must revert the changes made above.
    throw new TransactionExecutionException(errorCode, errorDescription);
  }

  @Override
  public String info() {
    return QaTransactionJson.toJson(ID, this);
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ValidErrorTx)) {
      return false;
    }
    ValidErrorTx that = (ValidErrorTx) o;
    return seed == that.seed
        && errorCode == that.errorCode
        && Objects.equals(errorDescription, that.errorDescription);
  }

  @Override
  public int hashCode() {
    return Objects.hash(seed, errorCode, errorDescription);
  }

  public static TransactionMessageConverter<ValidErrorTx> converter() {
    return Converter.INSTANCE;
  }

  private enum Converter implements TransactionMessageConverter<ValidErrorTx> {
    INSTANCE;

    @Override
    public ValidErrorTx fromRawTransaction(RawTransaction rawTransaction) {
      checkRawTransaction(rawTransaction);

      ValidErrorTxBody body = PROTO_SERIALIZER.fromBytes(rawTransaction.getPayload());
      long seed = body.getSeed();
      byte errorCode = (byte) body.getErrorCode();
      // Convert empty to null because unset error description will be deserialized
      // as empty string.
      String errorDescription = Strings.emptyToNull(body.getErrorDescription());
      return new ValidErrorTx(seed, errorCode, errorDescription);
    }

    @Override
    public RawTransaction toRawTransaction(ValidErrorTx transaction) {
      byte[] payload = PROTO_SERIALIZER.toBytes(ValidErrorTxBody.newBuilder()
          .setSeed(transaction.seed)
          .setErrorCode(transaction.errorCode)
          .setErrorDescription(transaction.errorDescription)
          .build());

      return RawTransaction.newBuilder()
          .serviceId(QaService.ID)
          .transactionId(ID)
          .payload(payload)
          .build();

    }

    private void checkRawTransaction(RawTransaction rawTransaction) {
      checkTransaction(rawTransaction, ID);
    }
  }

}
