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

extern crate exonum_testkit;
extern crate integration_tests;
extern crate java_bindings;

use java_bindings::exonum::helpers::fabric::ServiceFactory;
use java_bindings::JavaServiceFactoryAdapter;

#[test]
fn test_callbacks() {
    const CMD_RUN: &str = "run";

    let mut factory_adapter =
        JavaServiceFactoryAdapter::new("service_name".to_owned(), "artifact_path".to_owned());
    assert_eq!(factory_adapter.service_name(), "service_name");

    // Make sure it returns None for all commands except `Run`.
    assert!(factory_adapter.command("generate-template").is_none());
    assert!(factory_adapter.command("generate-config").is_none());
    assert!(factory_adapter.command("generate-testnet").is_none());
    assert!(factory_adapter.command("finalize").is_none());
    assert!(factory_adapter.command("run-dev").is_none());
    assert!(factory_adapter.command("generate-template").is_none());

    // Make sure it always returns Some for the `Run` command.
    let run_command_ext = factory_adapter.command("run");
    assert!(run_command_ext.is_some());
    assert!(factory_adapter.command(CMD_RUN).is_some());
    assert!(factory_adapter.command(CMD_RUN).is_some());

    let run_command_ext = run_command_ext.unwrap();
    let arguments = run_command_ext.args();
    assert_eq!(arguments.len(), 7);

    // Make sure another instance of JavaServiceFactoryAdapter does not extend the `Run` command.
    let mut one_more_adapter =
        JavaServiceFactoryAdapter::new("service_name2".to_owned(), "artifact_path2".to_owned());
    assert!(one_more_adapter.command(CMD_RUN).is_none());
    assert!(one_more_adapter.command(CMD_RUN).is_none());
}
