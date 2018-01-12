use jni::*;
use jni::errors::Result;
use jni::objects::AutoLocal;
use jni::objects::GlobalRef;
use jni::objects::JValue;
use jni::sys::jint;

use proxy::abc::Executor;


/// A temporary example of a native-to-JNI proxy
#[derive(Clone)]
pub struct AtomicIntegerProxy<E> where E: Executor {
    exec: E,
    obj: GlobalRef,
}

impl <E> AtomicIntegerProxy<E> where E: Executor {
    /// Creates a new instance of `AtomicIntegerProxy`
    pub fn new(exec: E, init_value: jint) -> Result<Self> {
        let obj = exec.with_attached(|env: &JNIEnv| {
            let local_ref = AutoLocal::new(env, env.new_object(
                "java/util/concurrent/atomic/AtomicInteger",
                "(I)V",
                &[JValue::from(init_value)]
            )?);
            env.new_global_ref(local_ref.as_obj())
        })?;
        Ok(AtomicIntegerProxy {
            exec,
            obj
        })
    }

    /// Gets a current value from java object
    pub fn get(&mut self) -> Result<jint> {
        self.exec.with_attached(|env| {
            env.call_method(self.obj.as_obj(), "get", "()I", &[])?.i()
        })
    }

    /// Increments a value of java object and then gets it
    pub fn increment_and_get(&mut self) -> Result<jint> {
        self.exec.with_attached(|env| {
            env.call_method(self.obj.as_obj(), "incrementAndGet", "()I", &[])?.i()
        })
    }

    /// Adds some value to the value of java object and then gets a resulting value
    pub fn add_and_get(&mut self, delta: jint) -> Result<jint> {
        let delta = JValue::from(delta);
        self.exec.with_attached(|env| {
            env.call_method(self.obj.as_obj(), "addAndGet", "(I)I", &[delta])?.i()
        })
    }
}


#[cfg(test)]
mod tests {
    use std::sync::{Arc, Barrier};
    use std::thread::spawn;
    use test_util::VM;
    use DumbExecutor;
    use super::*;

    #[test]
    pub fn it_works() {
        let executor = DumbExecutor { vm: VM.clone() };
        let mut atomic = AtomicIntegerProxy::new(executor, 0).unwrap();
        assert_eq!(0, atomic.get().unwrap());
        assert_eq!(1, atomic.increment_and_get().unwrap());
        assert_eq!(3, atomic.add_and_get(2).unwrap());
        assert_eq!(3, atomic.get().unwrap());
    }

    #[test]
    pub fn it_works_in_another_thread() {
        let executor = DumbExecutor { vm: VM.clone() };
        let mut atomic = AtomicIntegerProxy::new(executor, 0).unwrap();
        assert_eq!(0, atomic.get().unwrap());
        let jh = spawn(move || {
            assert_eq!(1, atomic.increment_and_get().unwrap());
            assert_eq!(3, atomic.add_and_get(2).unwrap());
            atomic
        });
        let mut atomic = jh.join().unwrap();
        assert_eq!(3, atomic.get().unwrap());
    }

    #[test]
    pub fn it_works_in_concurrent_threads() {
        const ITERS_PER_THREAD: usize = 10_000;
        const THREAD_NUM: usize = 8;

        let executor = DumbExecutor { vm: VM.clone() };
        let mut atomic = AtomicIntegerProxy::new(executor, 0).unwrap();
        let barrier = Arc::new(Barrier::new(THREAD_NUM));
        let mut threads = Vec::new();

        for _ in 0..THREAD_NUM {
            let barrier = barrier.clone();
            let mut atomic = atomic.clone();
            let jh = spawn(move || {
                barrier.wait();
                for _ in 0..ITERS_PER_THREAD {
                    atomic.increment_and_get().unwrap();
                }
            });
            threads.push(jh);
        }
        for jh in threads {
            jh.join().unwrap();
        }
        let expected = (ITERS_PER_THREAD * THREAD_NUM) as jint;
        assert_eq!(expected, atomic.get().unwrap());
    }
}
