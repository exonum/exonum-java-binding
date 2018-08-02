use std::fmt;

/// JavaServiceRuntime configuration.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Config {
    /// Private part of the EJB configuration parameters.
    pub private_config: PrivateConfig,
    /// Public part of the EJB configuration parameters.
    pub public_config: PublicConfig,
}

/// Private EJB configuration.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PrivateConfig {
    /// Additional parameters for JVM.
    ///
    /// Passed directly to JVM while initializing EJB runtime.
    /// Parameters must not have dash at the beginning.
    /// Some parameters are forbidden for setting up by user.
    pub user_parameters: Vec<String>,
    /// Java bindings framework system classpath.
    pub system_class_path: String,
    /// Java service classpath.
    pub service_class_path: String,
    /// Path to `log4j` configuration file.
    pub log_config_path: String,
    /// A port of the HTTP server for Java services. Must be distinct from the ports used by Exonum.
    pub port: i32,
}

/// Public EJB configuration.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PublicConfig {
    /// Fully qualified service module name.
    ///
    /// Must be subclass of `AbstractModule` and contain no-arguments constructor.
    pub module_name: String,
}

/// Error returned while validating user-specified additional parameters for JVM.
/// Trying to specify a parameter that is set by EJB internally.
#[derive(PartialEq)]
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
    if user_parameter.starts_with("Djava.class.path")
        || user_parameter.starts_with("Djava.library.path")
        || user_parameter.starts_with("Dlog4j.configurationFile")
    {
        Err(ForbiddenParameterError(user_parameter.to_string()))
    } else {
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn not_forbidden_debug() {
        let validation_result = validate_and_convert("Xdebug");
        assert_eq!(validation_result, Ok("-Xdebug".to_string()));
    }

    #[test]
    fn not_forbidden_user_parameter() {
        let validation_result = validate_and_convert("Duser.parameter=Djava.class.path");
        assert_eq!(
            validation_result,
            Ok("-Duser.parameter=Djava.class.path".to_string())
        );
    }

    #[test]
    #[should_panic(expected = "Trying to specify JVM parameter")]
    fn class_path() {
        validate_and_convert("Djava.class.path=target").unwrap();
    }

    #[test]
    #[should_panic(expected = "Trying to specify JVM parameter")]
    fn library_path() {
        validate_and_convert("Djava.library.path=some-dir").unwrap();
    }

    #[test]
    #[should_panic(expected = "Trying to specify JVM parameter")]
    fn log_config() {
        validate_and_convert("Dlog4j.configurationFile=logfile").unwrap();
    }
}
