package com.exonum.binding.fakes.runtimes;

import com.exonum.binding.core.proxy.CloseFailuresException;
import com.exonum.binding.core.runtime.ServiceLoadingException;
import com.exonum.binding.core.runtime.ServiceRuntime;
import com.exonum.binding.core.runtime.ServiceRuntimeAdapter;
import com.exonum.binding.core.runtime.ViewFactory;
import com.exonum.binding.core.transaction.TransactionExecutionException;

public class FakeRuntimeThrowingExceptions extends ServiceRuntimeAdapter {

    public FakeRuntimeThrowingExceptions() {
        super(null, null);
    }

    @Override
    protected void deployArtifact(String id, byte[] deploySpec) throws ServiceLoadingException {
        throw new ServiceLoadingException("deployArtifact");
    }

    @Override
    protected boolean isArtifactDeployed(String name) {
        throw new RuntimeException("isArtifactDeployed");
    }

    @Override
    protected void executeTransaction(int serviceId, int txId, byte[] arguments,
                                      long forkNativeHandle, byte[] txMessageHash, byte[] authorPublicKey)
            throws TransactionExecutionException, CloseFailuresException {
        throw new TransactionExecutionException(Byte.MAX_VALUE);
    }

    @Override
    protected void initialize(long nodeNativeHandle) {
        // do nothing
    }

    @Override
    protected void shutdown() throws InterruptedException {
        // do nothing
    }
}
