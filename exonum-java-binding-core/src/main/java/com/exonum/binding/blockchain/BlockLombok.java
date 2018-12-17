package com.exonum.binding.blockchain;

import com.exonum.binding.common.hash.HashCode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class BlockLombok {

    /**
     * Identifier of the leader node which has proposed the block.
     */
    private final int proposerId;

    /**
     * Height of the block, which also identifies the number of this particular block in the
     * blockchain starting from 0 ("genesis" block).
     */
    private final long height;

    /**
     * Number of transactions in this block.
     */
    private final int numTransactions;

    /**
     * Hash link to the previous block in the blockchain.
     */
    private final HashCode previousBlockHash;

    /**
     * Root hash of the Merkle tree of transactions in this block.
     * These transactions can be accesed with {@link Blockchain#getBlockTransactions(Block)}.
     */
    private final HashCode txRootHash;

    /**
     * Hash of the blockchain state after applying transactions in the block.
     */
    private final HashCode stateHash;

    /**
     * Returns the SHA-256 hash of this block.
     */
    private final HashCode blockHash;

    @Override
    public int hashCode() {
        // Use just the first 4 bytes of the SHA-256 hash of the binary object representation,
        // as they will have close to uniform distribution.
        // AutoValue will still use all fields in #equals.
        return blockHash.hashCode();
    }

}
