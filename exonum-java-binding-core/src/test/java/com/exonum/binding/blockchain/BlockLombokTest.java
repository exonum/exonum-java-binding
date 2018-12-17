package com.exonum.binding.blockchain;

import com.exonum.binding.common.hash.HashCode;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.exonum.binding.common.serialization.json.JsonSerializer.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.*;

class BlockLombokTest {

    private static final String BLOCK_EXAMPLE = "{\n"
            + "\"proposer_id\": \"1\",\n"
            + "\"height\": \"1\",\n"
            + "\"num_transactions\": \"1\",\n"
            + "\"previous_block_hash\": \"ab\",\n"
            + "\"tx_root_hash\": \"ab\",\n"
            + "\"state_hash\": \"ab\",\n"
            + "\"block_hash\": \"ab\"\n"
            + "}";

    @Test
    void roundTripTest() {
        BlockLombok block = createBlockLombok();
        BlockLombok restoredBlock = json().fromJson(
                json().toJson(block), BlockLombok.class);

        MatcherAssert.assertThat(restoredBlock, equalTo(block));
    }

    @Test
    void readBlock() {
        BlockLombok block = json().fromJson(BLOCK_EXAMPLE, BlockLombok.class);

        MatcherAssert.assertThat(block, notNullValue());
        MatcherAssert.assertThat(block.getProposerId(), is(1));
        MatcherAssert.assertThat(block.getHeight(), is(1L));
        MatcherAssert.assertThat(block.getNumTransactions(), is(1));
        MatcherAssert.assertThat(block.getPreviousBlockHash(), is(HashCode.fromString("ab")));
        MatcherAssert.assertThat(block.getTxRootHash(), is(HashCode.fromString("ab")));
        MatcherAssert.assertThat(block.getStateHash(), is(HashCode.fromString("ab")));
        MatcherAssert.assertThat(block.getBlockHash(), is(HashCode.fromString("ab")));
    }

    @Test
    void builderTest() {
        // Builder usage
        BlockLombok blockLombok = createBlockLombok();

        assertThat(blockLombok.getHeight()).isEqualTo(1L);

        // If we want to create copies or near-copies
        BlockLombok.BlockLombokBuilder builder = blockLombok.toBuilder();
        BlockLombok anotherBlockLombock = builder
                .height(2L)
                .build();

        assertThat(anotherBlockLombock.getHeight()).isEqualTo(2L);
    }

    private BlockLombok createBlockLombok() {
        return BlockLombok.builder()
                .proposerId(1)
                .height(1L)
                .numTransactions(1)
                .previousBlockHash(HashCode.fromString("ab"))
                .txRootHash(HashCode.fromString("ab"))
                .stateHash(HashCode.fromString("ab"))
                .blockHash(HashCode.fromString("ab"))
                .build();
    }
}
