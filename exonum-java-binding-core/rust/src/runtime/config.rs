/// JavaServiceRuntime configuration.
pub struct Config {
    /// JVM configuration.
    pub jvm_config: JvmConfig,
    /// Java service configuration.
    pub service_config: ServiceConfig,
}

/// JVM configuration.
pub struct JvmConfig {
    /// Whether to create JVM with `Xdebug` option or not.
    pub debug: bool,
    /// Optional classpath.
    pub class_path: Option<String>,
}

/// Java service configuration.
pub struct ServiceConfig {
    /// Fully qualified service module name.
    ///
    /// Must be subclass of `AbstractModule` and contain no-arguments constructor.
    pub module_name: String,
    /// Port number for server.
    pub port: i32,
}
