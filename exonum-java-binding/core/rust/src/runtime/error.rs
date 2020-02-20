// Copyright 2019 The Exonum Team
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use exonum::runtime::{ErrorKind, ExecutionError, ExecutionFail};
use jni::{
    objects::{GlobalRef, JObject},
    signature::{JavaType, Primitive},
    Executor, JNIEnv,
};

use crate::{
    utils::{
        describe_java_exception, get_and_clear_java_exception, get_exception_cause,
        get_exception_message,
        jni_cache::{classes_refs, execution_exception},
        unwrap_jni,
    },
    JniError, JniErrorKind, JniResult,
};

/// List of possible Java runtime errors.
#[derive(Clone, Copy, Debug, Eq, Hash, Ord, PartialEq, PartialOrd, ExecutionFail)]
#[execution_fail(kind = "runtime")]
pub enum Error {
    /// Illegal argument exception. E.g., as a result of malformed method arguments.
    IllegalArgument = 0,
    /// Java exception in runtime implementation. Is a result of a bug in the
    /// framework code.
    JavaException = 1,
    /// Any JNI error (except for Java exception). Is a result of bug in the
    /// JNI glue code or some low-level JVM-related issue.
    JniError = 2,
    /// Not supported operation.
    NotSupportedOperation = 3,
}

type ExceptionHandler = dyn Fn(&JNIEnv, JObject) -> ExecutionError;

mod exception_handlers {
    use super::*;

    pub const DEFAULT: &ExceptionHandler = &|env, exception| {
        let message = describe_java_exception(env, exception);
        Error::JavaException.with_description(message)
    };

    pub const TX_EXECUTION: &ExceptionHandler = &|env, exception| {
        let code = unwrap_jni(get_tx_error_code(env, exception)) as u8;
        let message = unwrap_jni(get_exception_message(env, exception)).unwrap_or_default();
        ExecutionError::service(code, message)
    };

    pub const TX_UNEXPECTED: &ExceptionHandler = &|env, exception| {
        let cause = unwrap_jni(get_exception_cause(env, exception));
        debug_assert!(
            !cause.is_null(),
            "UnexpectedExecutionException#getCause returned null"
        );
        let message = unwrap_jni(get_exception_message(env, cause)).unwrap_or_default();
        ExecutionError::new(ErrorKind::Unexpected, message)
    };

    pub const ILLEGAL_ARGUMENT: &ExceptionHandler = &|env, exception| {
        let message = unwrap_jni(get_exception_message(env, exception)).unwrap_or_default();
        Error::IllegalArgument.with_description(message)
    };
}

/// Executes closure `f` and handles any type of JNI errors from it. Occurred
/// exceptions are cleared.
///
/// Used for runtime methods results of which do *not* depend on
/// implementation of the user service, but rather on implementation of
/// Java runtime and user input. E.g., `deploy_artifact`.
///
/// - Any JNI errors are converted into `ExecutionError::Runtime(JniError)`.
/// - `IllegalArgumentException`s are converted into
///   `ExecutionError::Runtime(IllegalArgument)`.
/// - Any other exceptions are converted into
///   `ExecutionError::Runtime(JavaException)`.
pub fn jni_call_default<F, R>(executor: &Executor, f: F) -> Result<R, ExecutionError>
where
    F: FnOnce(&JNIEnv) -> JniResult<R>,
{
    jni_call::<F, &ExceptionHandler, R>(
        executor,
        &[(
            &classes_refs::java_lang_illegal_argument_exception(),
            exception_handlers::ILLEGAL_ARGUMENT,
        )],
        f,
    )
}

/// Executes closure `f` and handles any type of JNI errors from it. Occurred
/// exceptions are cleared.
///
/// Used for runtime methods results of which depend on
/// implementation of the user service, such as `execute`,
/// `before_transactions` and `after_transactions`.
///
/// - Any JNI errors are converted into `ExecutionError::Runtime(JniError)`.
/// - `IllegalArgumentException`s are converted into
///   `ExecutionError::Runtime(IllegalArgument)`.
/// - `ExecutionException`s are converted into
///   `ExecutionError::Service`.
/// - `UnexpectedExecutionException`s are converted into
///   `ExecutionError::Unexpected`.
/// - Any other exceptions are converted into
///   `ExecutionError::Runtime(JavaException)`.
pub fn jni_call_transaction<F, R>(executor: &Executor, f: F) -> Result<R, ExecutionError>
where
    F: FnOnce(&JNIEnv) -> JniResult<R>,
{
    jni_call::<F, &ExceptionHandler, R>(
        executor,
        &[
            (
                &classes_refs::execution_exception(),
                exception_handlers::TX_EXECUTION,
            ),
            (
                &classes_refs::unexpected_execution_exception(),
                exception_handlers::TX_UNEXPECTED,
            ),
            (
                &classes_refs::java_lang_illegal_argument_exception(),
                exception_handlers::ILLEGAL_ARGUMENT,
            ),
        ],
        f,
    )
}

/// Executes closure `f` and handles any type of JNI errors from it. Occurred
/// exceptions are cleared.
///
/// - Any JNI errors are converted into `ExecutionError::Runtime(JniError)`.
/// - Any Java exceptions are passed to corresponding `ExceptionHandler`
/// according to their type and `exception_handlers` mapping.
/// `exception_handlers::DEFAULT` is called in case of there are no handlers
/// or no handlers matched the exception type.
fn jni_call<F, H, R>(
    executor: &Executor,
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
    let result = executor.with_attached(|env| match f(env) {
        Ok(value) => Ok(Some(value)),
        Err(err) => {
            execution_error = Some(handle_error_or_exception::<H, R>(
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
            Err(err) => {
                Err(Error::JniError.with_description(format!("Unexpected JNI error: {:?}", err)))
            }
        },
        Some(error) => Err(error),
    }
}

/// Handles and clears any Java exceptions or other JNI errors.
///
/// Any JNI errors are converted into `ExecutionError` with their descriptions, for JNI errors
/// like `JniErrorKind::JavaException` it gets (and clears) any exception that is currently
/// being thrown, then exception is passed to corresponding `ExceptionHandler` according their
/// type and `exception_handlers` mapping.
/// `exception_handlers::DEFAULT` is called in case of there is no any handlers or handlers are
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

            exception_handlers::DEFAULT(env, exception)
        }
        _ => Error::JniError.with_description(err.to_string()),
    }
}

/// Returns the error code of the `ExecutionException`.
fn get_tx_error_code(env: &JNIEnv, exception: JObject) -> JniResult<i8> {
    assert!(!exception.is_null(), "Exception is null");
    let err_code = env.call_method_unchecked(
        exception,
        execution_exception::get_error_code_id(),
        JavaType::Primitive(Primitive::Byte),
        &[],
    )?;
    err_code.b()
}
