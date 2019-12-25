/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

use exonum::{
    blockchain::Blockchain,
    crypto::{Hash, PublicKey},
    exonum_merkledb::Snapshot,
    messages::BinaryValue,
    runtime::{
        ArtifactId, CallInfo, Caller, ExecutionContext, ExecutionError, ExecutionFail, InstanceId,
        InstanceSpec, Mailbox, Runtime, RuntimeIdentifier, SnapshotExt, WellKnownRuntime,
    },
};
use futures::{Future, IntoFuture};
use jni::{
    objects::{GlobalRef, JObject, JValue},
    signature::{JavaType, Primitive},
    sys::jint,
    Executor, JNIEnv,
};

use std::fmt;

use {
    runtime::Error,
    storage::View,
    to_handle,
    utils::{
        describe_java_exception, get_and_clear_java_exception, get_exception_message,
        jni_cache::{classes_refs, runtime_adapter, tx_execution_exception},
        panic_on_exception, unwrap_jni,
    },
    JniError, JniErrorKind, JniResult, Node,
};

/// Default validator ID. -1 is used as not-a-value in Java runtime.
const DEFAULT_VALIDATOR_ID: i32 = -1;
/// Java Runtime ID.
pub const JAVA_RUNTIME_ID: u32 = RuntimeIdentifier::Java as u32;

/// A proxy for `ServiceRuntimeAdapter`s.
#[derive(Clone)]
pub struct JavaRuntimeProxy {
    exec: Executor,
    runtime_adapter: GlobalRef,
    blockchain: Option<Blockchain>,
}

impl JavaRuntimeProxy {
    /// Creates new `JavaRuntimeProxy` for given `ServiceRuntimeAdapter` object
    pub fn new(executor: Executor, adapter: GlobalRef) -> Self {
        JavaRuntimeProxy {
            exec: executor,
            runtime_adapter: adapter,
            blockchain: None,
        }
    }

    fn parse_artifact(&self, artifact: &ArtifactId) -> Result<JavaArtifactId, ExecutionError> {
        if artifact.runtime_id != JAVA_RUNTIME_ID {
            Err(Error::IncorrectArtifactId.into())
        } else {
            Ok(JavaArtifactId(artifact.name.to_string()))
        }
    }

    /// If the current node is a validator, returns its ID, otherwise returns `-1`.
    fn validator_id(snapshot: &dyn Snapshot, pub_key: &PublicKey) -> i32 {
        snapshot
            .for_core()
            .consensus_config()
            .find_validator(|validator_keys| *pub_key == validator_keys.service_key)
            .map_or(DEFAULT_VALIDATOR_ID, |id| i32::from(id.0))
    }

    /// Handles and clears any Java exceptions or other JNI errors.
    ///
    /// Any JNI errors are converted into `ExecutionError` with their descriptions, for JNI errors
    /// like `JniErrorKind::JavaException` it gets (and clears) any exception that is currently
    /// being thrown, then exception is passed to corresponding `ExceptionHandler` according their
    /// type and `exception_handlers` mapping.
    /// `ExceptionHandlers::DEFAULT` is called in case of there is no any handlers or handlers are
    /// not matched to exception type.
    fn handle_error_or_exception<H, R>(
        env: &JNIEnv,
        err: JniError,
        exception_handlers: &[(&GlobalRef, H)],
    ) -> ExecutionError
    where
        H: Fn(&JNIEnv, JObject) -> ExecutionError,
    {
        match err.kind() {
            JniErrorKind::JavaException => {
                let exception = get_and_clear_java_exception(env);
                for (class, handler) in exception_handlers {
                    if unwrap_jni(env.is_instance_of(exception, *class)) {
                        return handler(env, exception);
                    }
                }

                ExceptionHandlers::DEFAULT(env, exception)
            }
            _ => Error::OtherJniError.with_description(err.to_string()),
        }
    }

    /// Executes closure `f` and handles any type of JNI errors from it.
    ///
    /// Any JNI errors are converted into `ExecutionError` with their descriptions, for JNI errors
    /// like `JniErrorKind::JavaException` it gets (and clears) any exception that is currently
    /// being thrown, `ExceptionHandlers::DEFAULT` is applied for such exceptions.
    fn jni_call_default<F, R>(&self, f: F) -> Result<R, ExecutionError>
    where
        F: FnOnce(&JNIEnv) -> JniResult<R>,
    {
        self.jni_call::<F, &ExceptionHandler, R>(&[], f)
    }

    /// Executes closure `f` and handles any type of JNI errors from it.
    ///
    /// Any JNI errors are converted into `ExecutionError` with their descriptions, for JNI errors
    /// like `JniErrorKind::JavaException` it gets (and clears) any exception that is currently
    /// being thrown, then exception is passed to corresponding `ExceptionHandler` according their
    /// type and `exception_handlers` mapping.
    /// `ExceptionHandlers::DEFAULT` is called in case of there is no any handlers or handlers are
    /// not matched to exception type.
    fn jni_call<F, H, R>(
        &self,
        exception_handlers: &[(&GlobalRef, H)],
        f: F,
    ) -> Result<R, ExecutionError>
    where
        F: FnOnce(&JNIEnv) -> JniResult<R>,
        H: Fn(&JNIEnv, JObject) -> ExecutionError,
    {
        let mut execution_error: Option<ExecutionError> = None;

        // Any errors or exceptions from `f` closure (managed native or java code)
        // will be handled by `Self::handle_error_or_exception` and stored as `execution_error`,
        // `result` will be solely `Ok` in such case;
        // Other errors (from jni_rs or JVM) are unexpected, they will be returned exclusively
        // as `JniResult`
        let result = self.exec.with_attached(|env| match f(env) {
            Ok(value) => Ok(Some(value)),
            Err(err) => {
                execution_error = Some(Self::handle_error_or_exception::<H, R>(
                    env,
                    err,
                    exception_handlers,
                ));
                Ok(None)
            }
        });

        match execution_error {
            None => match result {
                Ok(result) => {
                    assert!(result.is_some());
                    Ok(result.unwrap())
                }
                Err(err) => Err(Error::OtherJniError
                    .with_description(format!("Unexpected JNI error: {:?}", err))),
            },
            Some(error) => Err(error),
        }
    }
}

impl Runtime for JavaRuntimeProxy {
    fn initialize(&mut self, blockchain: &Blockchain) {
        self.blockchain = Some(blockchain.clone());

        unwrap_jni(self.exec.with_attached(|env| {
            let node_handle = to_handle(Node::new(blockchain.clone()));

            env.call_method_unchecked(
                self.runtime_adapter.as_obj(),
                runtime_adapter::initialize_id(),
                JavaType::Primitive(Primitive::Void),
                &[JValue::from(node_handle)],
            )
            .and_then(JValue::v)
        }))
    }

    fn deploy_artifact(
        &mut self,
        artifact: ArtifactId,
        deploy_spec: Vec<u8>,
    ) -> Box<dyn Future<Item = (), Error = ExecutionError>> {
        let id = match self.parse_artifact(&artifact) {
            Ok(id) => id.to_string(),
            Err(err) => return Box::new(Err(err).into_future()),
        };

        let result = self.jni_call_default(|env| {
            let artifact_id = JObject::from(env.new_string(id)?);
            let spec = JObject::from(env.byte_array_from_slice(&deploy_spec)?);

            env.call_method_unchecked(
                self.runtime_adapter.as_obj(),
                runtime_adapter::deploy_artifact_id(),
                JavaType::Primitive(Primitive::Void),
                &[JValue::from(artifact_id), JValue::from(spec)],
            )
            .and_then(JValue::v)
        });

        Box::new(result.map(|_| ()).into_future())
    }

    fn is_artifact_deployed(&self, id: &ArtifactId) -> bool {
        let artifact = match self.parse_artifact(id) {
            Ok(id) => id.to_string(),
            Err(_) => {
                return false;
            }
        };

        unwrap_jni(self.exec.with_attached(|env| {
            let artifact_id = JObject::from(env.new_string(artifact)?);

            panic_on_exception(
                env,
                env.call_method_unchecked(
                    self.runtime_adapter.as_obj(),
                    runtime_adapter::is_artifact_deployed_id(),
                    JavaType::Primitive(Primitive::Boolean),
                    &[JValue::from(artifact_id)],
                ),
            )
            .z()
        }))
    }

    fn start_adding_service(
        &self,
        context: ExecutionContext<'_>,
        spec: &InstanceSpec,
        parameters: Vec<u8>,
    ) -> Result<(), ExecutionError> {
        let serialized_instance_spec: Vec<u8> = spec.to_bytes();

        self.jni_call_default(|env| {
            let fork_handle = to_handle(View::from_ref_mut_fork(context.fork));
            let instance_spec =
                JObject::from(env.byte_array_from_slice(&serialized_instance_spec)?);
            let configuration = JObject::from(env.byte_array_from_slice(&parameters)?);

            env.call_method_unchecked(
                self.runtime_adapter.as_obj(),
                runtime_adapter::start_adding_service_id(),
                JavaType::Primitive(Primitive::Void),
                &[
                    JValue::from(fork_handle),
                    JValue::from(instance_spec),
                    JValue::from(configuration),
                ],
            )
            .and_then(JValue::v)
        })
    }

    fn commit_service(
        &mut self,
        _snapshot: &dyn Snapshot,
        instance_spec: &InstanceSpec,
    ) -> Result<(), ExecutionError> {
        let serialized_instance_spec: Vec<u8> = instance_spec.to_bytes();
        self.jni_call_default(|env| {
            let instance_spec =
                JObject::from(env.byte_array_from_slice(&serialized_instance_spec)?);

            env.call_method_unchecked(
                self.runtime_adapter.as_obj(),
                runtime_adapter::commit_service_id(),
                JavaType::Primitive(Primitive::Void),
                &[JValue::from(instance_spec)],
            )
            .and_then(JValue::v)
        })
    }

    fn execute(
        &self,
        context: ExecutionContext,
        call_info: &CallInfo,
        arguments: &[u8],
    ) -> Result<(), ExecutionError> {
        // todo: Replace this abomination (8-parameter method, arguments that make sense only
        //   in some cases) with a single protobuf message or other alternative [ECR-3872]
        let tx_info: (InstanceId, Hash, PublicKey) = match context.caller {
            Caller::Transaction {
                hash: message_hash,
                author: author_pk,
            } => (0, message_hash, author_pk),
            Caller::Service {
                instance_id: caller_id,
            } => (caller_id, Hash::default(), PublicKey::default()),
            Caller::Blockchain => {
                return Err(Error::NotSupportedOperation.into());
            }
        };

        self.jni_call(
            &[(
                &classes_refs::transaction_execution_exception(),
                ExceptionHandlers::TX_EXECUTION,
            )],
            |env| {
                let service_id = call_info.instance_id as i32;
                let interface_name = JObject::from(env.new_string(context.interface_name)?);
                let tx_id = call_info.method_id as i32;
                let args = JObject::from(env.byte_array_from_slice(arguments)?);
                let view_handle = to_handle(View::from_ref_fork(context.fork));
                let caller_id = tx_info.0;
                let message_hash = tx_info.1.to_bytes();
                let message_hash = JObject::from(env.byte_array_from_slice(&message_hash)?);
                let author_pk = tx_info.2.to_bytes();
                let author_pk = JObject::from(env.byte_array_from_slice(&author_pk)?);

                env.call_method_unchecked(
                    self.runtime_adapter.as_obj(),
                    runtime_adapter::execute_tx_id(),
                    JavaType::Primitive(Primitive::Void),
                    &[
                        JValue::from(service_id),
                        JValue::from(interface_name),
                        JValue::from(tx_id),
                        JValue::from(args),
                        JValue::from(view_handle),
                        JValue::from(caller_id as jint),
                        JValue::from(message_hash),
                        JValue::from(author_pk),
                    ],
                )
                .and_then(JValue::v)
            },
        )
    }

    fn before_transactions(
        &self,
        _context: ExecutionContext,
        _instance_id: InstanceId,
    ) -> Result<(), ExecutionError> {
        // TODO(ECR-4016): implement
        Ok(())
    }

    fn after_transactions(
        &self,
        context: ExecutionContext,
        instance_id: InstanceId,
    ) -> Result<(), ExecutionError> {
        self.jni_call_default(|env| {
            let view_handle = to_handle(View::from_ref_mut_fork(context.fork));

            panic_on_exception(
                env,
                env.call_method_unchecked(
                    self.runtime_adapter.as_obj(),
                    runtime_adapter::after_transactions_id(),
                    JavaType::Primitive(Primitive::Void),
                    &[JValue::from(instance_id as i32), JValue::from(view_handle)],
                ),
            );
            Ok(())
        })
    }

    fn after_commit(&mut self, snapshot: &dyn Snapshot, _mailbox: &mut Mailbox) {
        unwrap_jni(self.exec.with_attached(|env| {
            let view_handle = to_handle(View::from_ref_snapshot(snapshot));
            let public_key = self
                .blockchain
                .as_ref()
                .expect("afterCommit called before initialize")
                .service_keypair()
                .0;
            let validator_id = Self::validator_id(snapshot, &public_key);
            let height: u64 = snapshot.for_core().height().into();

            panic_on_exception(
                env,
                env.call_method_unchecked(
                    self.runtime_adapter.as_obj(),
                    runtime_adapter::after_commit_id(),
                    JavaType::Primitive(Primitive::Void),
                    &[
                        JValue::from(view_handle),
                        JValue::from(validator_id),
                        JValue::from(height as i64),
                    ],
                ),
            );
            Ok(())
        }));
    }

    fn shutdown(&mut self) {
        unwrap_jni(self.exec.with_attached(|env| {
            panic_on_exception(
                env,
                env.call_method_unchecked(
                    self.runtime_adapter.as_obj(),
                    runtime_adapter::shutdown_id(),
                    JavaType::Primitive(Primitive::Void),
                    &[],
                ),
            );
            Ok(())
        }));
    }
}

impl fmt::Debug for JavaRuntimeProxy {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "JavaRuntimeProxy()")
    }
}

impl WellKnownRuntime for JavaRuntimeProxy {
    const ID: u32 = JAVA_RUNTIME_ID;
}

/// Artifact identification properties within `JavaRuntimeProxy`
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct JavaArtifactId(String);

impl fmt::Display for JavaArtifactId {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}

type ExceptionHandler = Fn(&JNIEnv, JObject) -> ExecutionError;
struct ExceptionHandlers;

impl ExceptionHandlers {
    const DEFAULT: &'static ExceptionHandler = &|env, exception| {
        assert!(!exception.is_null(), "No exception thrown.");
        let message = describe_java_exception(env, exception);
        Error::JavaException.with_description(message)
    };

    const TX_EXECUTION: &'static ExceptionHandler = &|env, exception| {
        assert!(!exception.is_null(), "No exception thrown.");
        let code = unwrap_jni(Self::get_tx_error_code(env, exception)) as u8;
        let msg = unwrap_jni(get_exception_message(env, exception)).unwrap_or_default();
        ExecutionError::service(code, msg)
    };

    fn get_tx_error_code(env: &JNIEnv, exception: JObject) -> JniResult<i8> {
        let err_code = env.call_method_unchecked(
            exception,
            tx_execution_exception::get_error_code_id(),
            JavaType::Primitive(Primitive::Byte),
            &[],
        )?;
        err_code.b()
    }
}
