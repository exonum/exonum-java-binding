pub struct Config {
    pub jvm_config: JvmConfig,
    pub service_config: ServiceConfig,
}

pub struct JvmConfig {
    pub debug: bool,
    pub with_fakes: bool,
    pub library_path: String,
}

pub struct ServiceConfig {
    pub classpath: String,
    pub port: i32,
}
