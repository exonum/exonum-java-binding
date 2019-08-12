package com.exonum.binding.core.runtime;

import static com.exonum.binding.test.Bytes.bytes;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exonum.binding.common.hash.HashCode;
import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.service.BlockCommittedEvent;
import com.exonum.binding.core.service.adapters.ViewFactory;
import com.exonum.binding.core.storage.database.Snapshot;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// todo: This extension is currently not thread-safe. This test is always broken with
//  parallel execution. Remove everywhere once https://github.com/mockito/mockito/issues/1630
//  is resolved.
@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.SAME_THREAD) // MockitoExtension is not thread-safe: see mockito/1630
class ServiceRuntimeAdapterTest {

  @Test
  void convertStateHashesEmpty() {
    List<List<HashCode>> runtimeStateHashes = ImmutableList.of();
    byte[][][] expectedStateHashes = new byte[][][] {};

    assertThat(ServiceRuntimeAdapter.convertStateHashes(runtimeStateHashes))
        .isEqualTo(expectedStateHashes);
  }

  @Test
  void convertStateHashes() {
    HashCode h1 = HashCode.fromBytes(bytes(1, 2));
    HashCode h2 = HashCode.fromBytes(bytes(3, 4));
    HashCode h3 = HashCode.fromBytes(bytes(5, 6));
    List<List<HashCode>> runtimeStateHashes = ImmutableList.of(
        ImmutableList.of(),
        ImmutableList.of(h1),
        ImmutableList.of(h2, h3)
    );
    byte[][][] expectedStateHashes = new byte[][][] {
        new byte[][] {},
        new byte[][] {h1.asBytes()},
        new byte[][] {h2.asBytes(), h3.asBytes()},
    };

    // todo: Replace with Arrays.deepEquals if needed
    assertThat(ServiceRuntimeAdapter.convertStateHashes(runtimeStateHashes))
        .isEqualTo(expectedStateHashes);
  }

  @Nested
  class WithServiceRuntime {
    private static final long SNAPSHOT_HANDLE = 0x0A;
    private static final long HEIGHT = 1;
    private static final int VALIDATOR_ID = 1;

    @Mock
    private ServiceRuntime serviceRuntime;
    @Mock
    private ViewFactory viewFactory;
    private ServiceRuntimeAdapter serviceRuntimeAdapter;
    @Mock
    private Snapshot snapshot;

    @BeforeEach
    void setUp() {
      serviceRuntimeAdapter = new ServiceRuntimeAdapter(serviceRuntime, viewFactory);
    }

    @Test
    void afterCommit_ValidatorNode() throws CloseFailuresException {
      when(viewFactory.createSnapshot(eq(SNAPSHOT_HANDLE), any(Cleaner.class)))
          .thenReturn(snapshot);
      serviceRuntimeAdapter.afterCommit(SNAPSHOT_HANDLE, VALIDATOR_ID, HEIGHT);

      ArgumentCaptor<BlockCommittedEvent> ac = ArgumentCaptor.forClass(BlockCommittedEvent.class);
      verify(serviceRuntime).afterCommit(ac.capture());

      BlockCommittedEvent event = ac.getValue();

      assertThat(event.getHeight()).isEqualTo(HEIGHT);
      assertThat(event.getValidatorId()).hasValue(VALIDATOR_ID);
      assertThat(event.getSnapshot()).isEqualTo(snapshot);
    }

    @Test
    void afterCommit_AuditorNode() throws CloseFailuresException {
      // For auditor nodes (which do not have validatorId) negative validatorId is passed
      int validatorId = -1;
      when(viewFactory.createSnapshot(eq(SNAPSHOT_HANDLE), any(Cleaner.class)))
          .thenReturn(snapshot);
      serviceRuntimeAdapter.afterCommit(SNAPSHOT_HANDLE, validatorId, HEIGHT);

      ArgumentCaptor<BlockCommittedEvent> ac = ArgumentCaptor.forClass(BlockCommittedEvent.class);
      verify(serviceRuntime).afterCommit(ac.capture());

      BlockCommittedEvent event = ac.getValue();

      assertThat(event.getHeight()).isEqualTo(HEIGHT);
      assertThat(event.getValidatorId()).isEmpty();
    }
  }
}
