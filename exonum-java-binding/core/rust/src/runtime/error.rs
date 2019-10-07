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

//! The set of specific for the Java runtime implementation errors.
use exonum::runtime::{ErrorKind, ExecutionError};


/// List of possible Java runtime errors.
#[derive(Clone, Copy, Debug, Eq, Hash, Ord, PartialEq, PartialOrd, IntoExecutionError)]
#[exonum(crate = "crate", kind = "runtime")]
pub enum Error {
    /// Unable to parse artifact identifier or specified artifact has non-empty spec.
    IncorrectArtifactId = 0,
    /// Checked java exception is occurred
    JavaException = 1,
    /// Any JNI error is occurred (except Java exception)
    OtherJniError = 2,
    /// Not supported operation
    NotSupportedOperation = 3,
}
