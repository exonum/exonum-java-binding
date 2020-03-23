/*
 * Copyright 2020 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.core.blockchain;

import com.exonum.binding.common.blockchain.CallInBlocks;
import com.exonum.binding.core.storage.indices.MapIndexProxy;
import com.exonum.binding.core.storage.indices.ProofMapIndexProxy;
import com.exonum.messages.core.Blockchain.CallInBlock;
import com.exonum.messages.core.Proofs.CallProof;
import com.exonum.messages.core.runtime.Errors.ExecutionError;
import com.exonum.messages.core.runtime.Errors.ExecutionErrorAux;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Provides information about call errors within a specific block.
 *
 * <p>This data type can be used to get information or build proofs that execution of a certain
 * call ended up with a particular status.
 *
 * <p>Execution errors are preserved for transactions and before/after transaction handlers.
 *
 * <p>Use {@link Blockchain#getCallRecords(long)} to instantiate this class.
 */
public final class CallRecords {

  private final Blockchain blockchain;
  private final long blockHeight;
  private ProofMapIndexProxy<CallInBlock, ExecutionError> callErrors;
  private MapIndexProxy<CallInBlock, ExecutionErrorAux> callErrorsAux;

  CallRecords(CoreSchema schema, Blockchain blockchain, long blockHeight) {
    this.blockchain = blockchain;
    this.blockHeight = blockHeight;
    this.callErrors = schema.getCallErrors(blockHeight);
    this.callErrorsAux = schema.getCallErrorsAux(blockHeight);
  }

  /**
   * Returns all errors in the block.
   */
  public Map<CallInBlock, ExecutionError> getErrors() {
    var mergedErrors = new HashMap<CallInBlock, ExecutionError>();
    var callErrorsIter = callErrors.entries();
    while (callErrorsIter.hasNext()) {
      var callErrorEntry = callErrorsIter.next();
      var callId = callErrorEntry.getKey();
      var callError = merge(callErrorEntry.getValue(), callErrorsAux.get(callId));
      mergedErrors.put(callId, callError);
    }
    return mergedErrors;
  }

  /**
   * Finds the error, if any, occurred in the given call.
   *
   * @param callInBlock the call ID
   * @return an ExecutionError if one occurred in the given call in this block;
   *     or {@code Optional.empty()} if the call completed successfully <strong>or</strong>
   *     did not happen at all
   * @see CallInBlocks
   */
  public Optional<ExecutionError> get(CallInBlock callInBlock) {
    if (!callErrors.containsKey(callInBlock)) {
      return Optional.empty();
    }
    var callError = callErrors.get(callInBlock);
    var callErrorAuxInfo = callErrorsAux.get(callInBlock);
    // Merge the auxiliary info into the call error
    return Optional.of(merge(callError, callErrorAuxInfo));
  }

  private static ExecutionError merge(ExecutionError executionError,
      ExecutionErrorAux executionErrorAuxInfo) {
    return ExecutionError.newBuilder(executionError)
        .setDescription(executionErrorAuxInfo.getDescription())
        .build();
  }

  /**
   * Returns a cryptographic proof of authenticity for a top-level call within a block.
   *
   * @param callInBlock the call ID
   * @see <a href="Blockchain.html#call-result-proof">Call Result Proofs</a>
   * @see CallInBlocks
   */
  public CallProof getProof(CallInBlock callInBlock) {
    // Create the block proof
    var blockProof = blockchain.createBlockProof(blockHeight);

    // Create the error entry proof
    var callErrorProof = callErrors.getProof(callInBlock);

    // Create the description
    var description = callErrorsAux.containsKey(callInBlock)
        ? callErrorsAux.get(callInBlock).getDescription()
        : "";
    // todo: consider creating a wrapper for this proof for (a) consistency with other proofs
    //   and (b) to be able to provide extra operation (e.g., verification) atop
    return CallProof.newBuilder()
        .setBlockProof(blockProof.getAsMessage())
        .setCallProof(callErrorProof.getAsMessage())
        .setErrorDescription(description)
        .build();
  }
}
