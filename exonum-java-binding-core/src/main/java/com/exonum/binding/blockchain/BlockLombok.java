package com.exonum.binding.blockchain;

import com.exonum.binding.common.hash.HashCode;
import com.google.common.base.Objects;
import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class BlockLombok {

    /**
     * Identifier of the leader node which has proposed the block.
     */
    @SerializedName("proposer_id")
    private final int proposerId;

    /**
     * Height of the block, which also identifies the number of this particular block in the
     * blockchain starting from 0 ("genesis" block).
     */
    private final long height;

    /**
     * Number of transactions in this block.
     */
    @SerializedName("num_transactions")
    private final int numTransactions;

    /**
     * Hash link to the previous block in the blockchain.
     */
    @SerializedName("previous_block_hash")
    private final HashCode previousBlockHash;

    /**
     * Root hash of the Merkle tree of transactions in this block.
     * These transactions can be accesed with {@link Blockchain#getBlockTransactions(Block)}.
     */
    @SerializedName("tx_root_hash")
    private final HashCode txRootHash;

    /**
     * Hash of the blockchain state after applying transactions in the block.
     */
    @SerializedName("state_hash")
    private final HashCode stateHash;

    /**
     * Returns the SHA-256 hash of this block.
     */
    @SerializedName("block_hash")
    private final HashCode blockHash;

    @Override
    public int hashCode() {
        // Use just the first 4 bytes of the SHA-256 hash of the binary object representation,
        // as they will have close to uniform distribution.
        // AutoValue will still use all fields in #equals.
        return blockHash.hashCode();
    }

    // If we didn't override hashCode, both hashCode and equals would be generated as class is marked with @Data
    // annotation
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockLombok that = (BlockLombok) o;
        return proposerId == that.proposerId &&
                height == that.height &&
                numTransactions == that.numTransactions &&
                Objects.equal(previousBlockHash, that.previousBlockHash) &&
                Objects.equal(txRootHash, that.txRootHash) &&
                Objects.equal(stateHash, that.stateHash) &&
                Objects.equal(blockHash, that.blockHash);
    }
}
