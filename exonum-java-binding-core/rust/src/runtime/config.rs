use std::fmt;

/// JavaServiceRuntime configuration.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Config {
    /// JVM configuration.
    pub jvm_config: JvmConfig,
    /// Java service configuration.
    pub service_config: ServiceConfig,
}

/// JVM configuration.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JvmConfig {
    /// Additional parameters for JVM.
    ///
    /// Passed directly to JVM while initializing EJB runtime.
    /// Parameters must not have dash at the beginning.
    /// Some parameters are forbidden for setting up by user.
    pub user_parameters: Vec<String>,
    /// Java service classpath. Must include all its dependencies.
    ///
    /// Includes java_bindings internal dependencies as well as user service dependencies.
    pub class_path: String,
    /// Path to java-bindings shared library.
    ///
    /// Should be path to exonum-java-binding-core/rust/target/{debug, release}
    pub lib_path: String,
    /// Path to `log4j` configuration file.
    pub log_config_path: String,
}

/// Java service configuration.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ServiceConfig {
    /// Fully qualified service module name.
    ///
    /// Must be subclass of `AbstractModule` and contain no-arguments constructor.
    pub module_name: String,
    /// A port of the HTTP server for Java services. Must be distinct from the ports used by Exonum.
    pub port: i32,
}

/// Error returned while validating user-specified additional parameters for JVM.
/// Trying to specify a parameter that is set by EJB internally.
pub struct ForbiddenParameterError(String);

impl fmt::Debug for ForbiddenParameterError {
    fn fmt(&self, f: &mut fmt::Formatter) -> Result<(), fmt::Error> {
        write!(
            f,
            "Trying to specify JVM parameter [{}] that is set by EJB internally. \
             Use EJB parameters instead.",
            self.0
        )
    }
}

/// Checks if parameter is not in list of forbidden parameters and adds a dash at the beginning.
pub(crate) fn validate_and_convert(
    user_parameter: &str,
) -> Result<String, ForbiddenParameterError> {
    check_not_forbidden(user_parameter)?;
    // adding dash at the beginning
    let mut user_parameter = user_parameter.to_owned();
    user_parameter.insert(0, '-');

    Ok(user_parameter)
}

fn check_not_forbidden(user_parameter: &str) -> Result<(), ForbiddenParameterError> {
    if user_parameter.contains("Djava.class.path")
        || user_parameter.contains("Djava.library.path")
        || user_parameter.contains("log4j.configurationFile")
    {
        Err(ForbiddenParameterError(user_parameter.to_string()))
    } else {
        Ok(())
    }
}
