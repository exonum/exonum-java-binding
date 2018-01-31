use exonum::blockchain::Transaction;
use exonum::storage::Fork;
use exonum::messages::{Message, RawMessage};
use jni::*;
use jni::errors::Result;
use jni::objects::GlobalRef;
use jni::objects::JValue;

use std::fmt;

use Executor;
use storage::View;
use utils;

/// A proxy for `Transaction`s
#[derive(Clone)]
pub struct TransactionProxy<E>
where
    E: Executor + 'static,
{
    exec: E,
    adapter: GlobalRef,
}

// FIXME docs
// `TransactionProxy` is immutable, so it can be safely used in different threads.
unsafe impl <E> Sync for TransactionProxy<E>
where
    E: Executor + 'static,
{}

impl<E> fmt::Debug for TransactionProxy<E>
where
    E: Executor + 'static,
{
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "TransactionProxy")
    }
}

impl<E> TransactionProxy<E>
where
    E: Executor + 'static,
{
    /// Creates an instance of `TransactionProxy` with already prepared global ref
    /// to `UserTransactionAdapter`.
    pub unsafe fn from_global_ref(exec: E, adapter: GlobalRef) -> Self {
        TransactionProxy { exec, adapter }
    }
}

impl<E> Transaction for TransactionProxy<E>
where
    E: Executor + 'static,
{
    fn verify(&self) -> bool {
        println!("UserTransactionAdapter::verify()");
        let res = self.exec.with_attached(|env: &JNIEnv| {
            env.call_method(
                self.adapter.as_obj(),
                "isValid",
                "()Z",
                &[],
            )?
                .z()
        });
        // FIXME maybe here should be a panic?
        res.unwrap_or(false)
    }

    fn execute(&self, fork: &mut Fork) {
        println!("UserTransactionAdapter::execute()");
        let res = (|| -> Result<()> {
            self.exec.with_attached(|env: &JNIEnv| {
                let view_handle = utils::to_handle(View::from_ref_fork(fork));
                env.call_method(
                    self.adapter.as_obj(),
                    "execute",
                    "(J)V",
                    &[JValue::from(view_handle)],
                )?;
                Ok(())
            })
        })();
        // FIXME here should be a nice panic
        res.unwrap()
    }
}

impl<E> Message for TransactionProxy<E>
where
    E: Executor,
{
    fn raw(&self) -> &RawMessage {
        unimplemented!()
    }
}
