pub struct Config {
    pub jvm_config: JvmConfig,
    pub service_config: ServiceConfig,
}

pub struct JvmConfig {
    pub debug: bool,
}

pub struct ServiceConfig {
    pub classpath: String,
    pub port: i32,
}
