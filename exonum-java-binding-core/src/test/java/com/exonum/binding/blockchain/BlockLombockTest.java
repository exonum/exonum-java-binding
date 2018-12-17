package com.exonum.binding.blockchain;

import com.exonum.binding.common.hash.HashCode;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockLombockTest {

    @Test
    public void builderTest() {
        // Builder usage
        BlockLombok blockLombok = BlockLombok.builder()
                .proposerId(1)
                .height(1L)
                .numTransactions(1)
                .previousBlockHash(HashCode.fromString("ab"))
                .txRootHash(HashCode.fromString("ab"))
                .stateHash(HashCode.fromString("ab"))
                .blockHash(HashCode.fromString("ab"))
                .build();

        assertThat(blockLombok.getHeight()).isEqualTo(1L);

        // If we want to create copies or near-copies
        BlockLombok.BlockLombokBuilder builder = blockLombok.toBuilder();
        BlockLombok anotherBlockLombock = builder
                .height(2L)
                .build();

        assertThat(anotherBlockLombock.getHeight()).isEqualTo(2L);
    }
}
