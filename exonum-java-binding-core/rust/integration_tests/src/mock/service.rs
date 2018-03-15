use java_bindings::exonum::crypto::Hash;
use java_bindings::{Executor, ServiceProxy, TransactionProxy};
use java_bindings::jni::JNIEnv;
use java_bindings::jni::objects::{JObject, JValue, GlobalRef};
use java_bindings::utils::unwrap_jni;

pub struct ServiceMockBuilder<E>
where
    E: Executor + 'static,
{
    exec: E,
    builder: GlobalRef,
}

impl<E> ServiceMockBuilder<E>
where
    E: Executor + 'static,
{
    pub fn new(exec: E) -> Self {
        let builder = unwrap_jni(exec.with_attached(|env| {
            let value = env.call_static_method(
                "com/exonum/binding/fakes/services/NativeAdapterFakes",
                "createServiceFakeBuilder",
                "()Lcom/exonum/binding/service/adapters/UserServiceAdapterMockBuilder;",
                &[],
            )?;
            env.new_global_ref(env.auto_local(value.l()?).as_obj())
        }));
        ServiceMockBuilder { exec, builder }
    }

    pub fn id(self, id: u16) -> Self {
        unwrap_jni(self.exec.with_attached(|env| {
            env.call_method(
                self.builder.as_obj(),
                "id",
                "(S)V",
                &[JValue::from(id as i16)],
            )?;
            Ok(())
        }));
        self
    }

    pub fn name(self, name: String) -> Self {
        unwrap_jni(self.exec.with_attached(|env| {
            let name = env.new_string(name)?;
            env.call_method(
                self.builder.as_obj(),
                "name",
                "(Ljava/lang/String;)V",
                &[JValue::from(JObject::from(name))],
            )?;
            Ok(())
        }));
        self
    }

    pub fn converting_to(self, transaction: TransactionProxy<E>) -> Self {
        unimplemented!()
    }

    pub fn rejecting_raw_transactions(self) -> Self {
        unwrap_jni(self.exec.with_attached(|env| {
            env.call_method(
                self.builder.as_obj(),
                "rejectingRawTransactions",
                "()V",
                &[],
            )?;
            Ok(())
        }));
        self
    }

    pub fn state_hashes(self, hashes: &[Hash]) -> Self {
        unwrap_jni(self.exec.with_attached(|env| {
            let byte_array_array = unimplemented!();
            env.call_method(
                self.builder.as_obj(),
                "stateHashes",
                "([[B)V",
                &[JValue::from(byte_array_array)],
            )?;
            Ok(())
        }));
        self
    }

    pub fn initial_global_config(self, config: String) -> Self {
        unwrap_jni(self.exec.with_attached(|env| {
            let config = env.new_string(config)?;
            env.call_method(
                self.builder.as_obj(),
                "initialGlobalConfig",
                "(Ljava/lang/String;)V",
                &[JValue::from(JObject::from(config))],
            )?;
            Ok(())
        }));
        self
    }

    pub fn build(self) -> ServiceProxy<E> {
        let service = unwrap_jni(self.exec.with_attached(|env| {
            let value = env.call_method(
                self.builder.as_obj(),
                "build",
                "()Lcom/exonum/binding/service/adapters/UserServiceAdapter;",
                &[],
            )?;
            env.new_global_ref(env.auto_local(value.l()?).as_obj())
        }));

        ServiceProxy::from_global_ref(self.exec, service)
    }
}
