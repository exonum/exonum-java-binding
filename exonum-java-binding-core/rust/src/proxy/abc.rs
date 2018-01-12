use jni::JNIEnv;
use jni::errors::Result;


// FIXME human-friendly docs
/// An abstract interface for JNI thread attachment manager.
pub trait Executor: Clone {
    /// Executes a provided closure with an attached JNI thread
    fn with_attached<F, R>(&self, f: F) -> Result<R>
        where
            F: FnOnce(&JNIEnv) -> Result<R>;
}
