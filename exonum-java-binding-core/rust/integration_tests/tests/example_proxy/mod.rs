use java_bindings::Executor;
use jni::errors::Result;
use jni::objects::AutoLocal;
use jni::objects::GlobalRef;
use jni::objects::JValue;
use jni::sys::jint;
use jni::*;

/// A temporary example of a native-to-JNI proxy
#[derive(Clone)]
pub struct AtomicIntegerProxy<E>
where
    E: Executor,
{
    exec: E,
    obj: GlobalRef,
}

impl<E> AtomicIntegerProxy<E>
where
    E: Executor,
{
    /// Creates a new instance of `AtomicIntegerProxy`
    pub fn new(exec: E, init_value: jint) -> Result<Self> {
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
    pub fn get(&mut self) -> Result<jint> {
        self.exec
            .with_attached(|env| env.call_method(self.obj.as_obj(), "get", "()I", &[])?.i())
    }

    /// Increments a value of java object and then gets it
    pub fn increment_and_get(&mut self) -> Result<jint> {
        self.exec.with_attached(|env| {
            env.call_method(self.obj.as_obj(), "incrementAndGet", "()I", &[])?
                .i()
        })
    }

    /// Adds some value to the value of java object and then gets a resulting value
    pub fn add_and_get(&mut self, delta: jint) -> Result<jint> {
        let delta = JValue::from(delta);
        self.exec.with_attached(|env| {
            env.call_method(self.obj.as_obj(), "addAndGet", "(I)I", &[delta])?
                .i()
        })
    }
}
