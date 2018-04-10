use java_bindings::{JniExecutor, JniResult, MainExecutor};
use java_bindings::jni::JNIEnv;
use java_bindings::jni::objects::{AutoLocal, GlobalRef, JValue};
use java_bindings::jni::sys::jint;

/// A temporary example of a native-to-JNI proxy
#[derive(Clone)]
pub struct AtomicIntegerProxy {
    exec: MainExecutor,
    obj: GlobalRef,
}

impl AtomicIntegerProxy {
    /// Creates a new instance of `AtomicIntegerProxy`
    pub fn new(exec: MainExecutor, init_value: jint) -> JniResult<Self> {
        let obj = exec.with_attached(|env: &JNIEnv| {
            let local_ref = AutoLocal::new(
                env,
                env.new_object(
                    "java/util/concurrent/atomic/AtomicInteger",
                    "(I)V",
                    &[JValue::from(init_value)],
                )?,
            );
            env.new_global_ref(local_ref.as_obj())
        })?;
        Ok(AtomicIntegerProxy { exec, obj })
    }

    /// Gets a current value from java object
    pub fn get(&mut self) -> JniResult<jint> {
        self.exec.with_attached(|env| {
            env.call_method(self.obj.as_obj(), "get", "()I", &[])?.i()
        })
    }

    /// Increments a value of java object and then gets it
    pub fn increment_and_get(&mut self) -> JniResult<jint> {
        self.exec.with_attached(|env| {
            env.call_method(self.obj.as_obj(), "incrementAndGet", "()I", &[])?
                .i()
        })
    }

    /// Adds some value to the value of java object and then gets a resulting value
    pub fn add_and_get(&mut self, delta: jint) -> JniResult<jint> {
        let delta = JValue::from(delta);
        self.exec.with_attached(|env| {
            env.call_method(self.obj.as_obj(), "addAndGet", "(I)I", &[delta])?
                .i()
        })
    }
}
