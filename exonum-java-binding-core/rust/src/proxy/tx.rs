use exonum::crypto::PublicKey;
use exonum::blockchain::Transaction;
use exonum::storage::Fork;
use exonum::messages::{Message, RawMessage};
use jni::*;
use jni::errors::Result;
use jni::objects::AutoLocal;
use jni::objects::GlobalRef;
use jni::objects::JValue;
use jni::sys::jboolean;

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
    obj: GlobalRef,
}

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
    /// FIXME add documentation
    pub fn new(exec: E, fqn: &str) {
//        let obj = exec.with_attached(|env: &JNIEnv| {
//            let local_ref = AutoLocal::new(
//                env,
//                env.new_object(
//                    fqn,
//                    "(I)V",
//                    &[JValue::from(init_value)],
//                )?,
//            );
//            env.new_global_ref(local_ref.as_obj())
//        })?;
    }
}

impl<E> Transaction for TransactionProxy<E>
where
    E: Executor + 'static,
{
    fn verify(&self) -> bool {
        println!("TransactionProxy::verify()");
        let res = (|| -> Result<bool> {
            let _res = self.exec.with_attached(|env: &JNIEnv| {
                env.call_method(
                    self.obj.as_obj(),
                    "isValid",
                    "()Z",
                    &[],
                )?
                    .b()
            })?;
            // FIXME real result
            Ok(true)
        })();
        // FIXME maybe here should be panic?
        res.unwrap_or(false)
    }

    fn execute(&self, fork: &mut Fork) {
        println!("TransactionProxy::execute()");
        let res = (|| -> Result<()> {
            self.exec.with_attached(|env: &JNIEnv| {
                env.call_method(
                    self.obj.as_obj(),
                    "execute",
                    "()V",
                    // FIXME how this is intended to work?
                    &[JValue::from(utils::to_handle(View::Fork(fork)))],
/*
89 |                     &[JValue::from(utils::to_handle(View::Fork(fork)))],
   |                                                                ^^^^ expected struct `exonum::storage::Fork`, found mutable reference
*/
                )?;
                Ok(())
            })
        })();
        // FIXME here should be nice panic
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
