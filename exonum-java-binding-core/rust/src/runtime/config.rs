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
    /// Whether to create JVM with `Xdebug` option or not.
    pub debug: bool,
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
