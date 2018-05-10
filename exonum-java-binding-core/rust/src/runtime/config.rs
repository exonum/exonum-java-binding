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
    /// Сlasspath.
    pub class_path: String,
    /// Libpath.
    pub lib_path: String,
}

/// Java service configuration.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ServiceConfig {
    /// Fully qualified service module name.
    ///
    /// Must be subclass of `AbstractModule` and contain no-arguments constructor.
    pub module_name: String,
    /// Port number for server.
    pub port: i32,
}
