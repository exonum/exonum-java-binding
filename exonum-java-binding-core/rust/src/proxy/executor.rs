use std::sync::Arc;

use jni::*;
use jni::errors::Result;

use proxy::abc::Executor;


/// A "dumb" implementation of Executor.
/// It attaches to JNI thread and then detaches (poor performance)
#[derive(Clone)]
pub struct DumbExecutor {
    /// The invocation API
    pub vm: Arc<JavaVM>,
}

impl Executor for DumbExecutor {
    fn with_attached<F, R>(&self, f: F) -> Result<R>
        where
            F: FnOnce(&JNIEnv) -> Result<R>
    {
        let env = self.vm.attach_current_thread()?;
        f(&env)
    }
}
