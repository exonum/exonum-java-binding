use failure::{self, format_err};

use std::str::FromStr;

/// Mode of the Supervisor service.
#[derive(Debug, Copy, Clone, PartialEq, Serialize, Deserialize)]
pub enum SupervisorMode {
    /// Simple mode.
    Simple,
    /// Decentralized mode.
    Decentralized,
}

impl FromStr for SupervisorMode {
    type Err = failure::Error;

    fn from_str(input: &str) -> Result<Self, Self::Err> {
        match input.to_ascii_lowercase().as_str() {
            "simple" => Ok(SupervisorMode::Simple),
            "decentralized" => Ok(SupervisorMode::Decentralized),
            _ => Err(format_err!(
                "Invalid supervisor mode. Can be \"simple\" or \"decentralized\""
            )),
        }
    }
}

#[cfg(test)]
mod tests {
    use std::str::FromStr;
    use SupervisorMode;

    #[test]
    fn simple_mode_from_str() {
        let input = "simple";
        let mode = SupervisorMode::from_str(input).unwrap();
        assert_eq!(mode, SupervisorMode::Simple);
    }

    #[test]
    fn decentralized_mode_from_str() {
        let input = "decentralized";
        let mode = SupervisorMode::from_str(input).unwrap();
        assert_eq!(mode, SupervisorMode::Decentralized);
    }

    #[test]
    fn invalid_mode_from_str() {
        let input = "invalid_mode";
        let err = SupervisorMode::from_str(input).unwrap_err();
        assert!(err.to_string().contains("Invalid supervisor mode"));
    }
}
