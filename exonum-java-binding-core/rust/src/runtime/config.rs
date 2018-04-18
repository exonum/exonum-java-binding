pub struct Config {
    pub jvm_config: JvmConfig,
    pub service_config: ServiceConfig,
}

pub struct JvmConfig {
    pub debug: bool,
    pub classpath: Option<String>,
}

pub struct ServiceConfig {
    pub module_name: String,
    pub port: i32,
}
