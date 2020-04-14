# Package Exonum Java app on Windows
# TODO(ECR-4309): run tests if --skip-tests flag is not supplied?
# TODO(ECR-4309): add release build support
# TODO(ECR-4309): error handling

Set-StrictMode -Version 1.0
$ErrorActionPreference = "stop"

try
{
    . .\tests_profile.ps1

    # Clear Rust target directory
    # TODO(ECR-4309): option --skip-cargo-clean
    $EJB_RUST_DIR = "$PROJECT_ROOT\core\rust"
    # & cargo clean --manifest-path="$EJB_RUST_DIR\Cargo.toml"

    # Prepare directories for native libraries and additional files
    $PACKAGING_BASE_DIR = "$EJB_RUST_DIR\target\debug"
    $PACKAGING_ETC_DIR = "$PACKAGING_BASE_DIR\etc"
    $PACKAGING_NATIVE_LIB_DIR = "${PACKAGING_BASE_DIR}\lib\native"
    if (!(Test-Path $PACKAGING_BASE_DIR))
    {
        New-Item -ItemType Directory -Force -Path $PACKAGING_BASE_DIR
    }
    if (!(Test-Path $PACKAGING_NATIVE_LIB_DIR))
    {
        New-Item -ItemType Directory -Force -Path $PACKAGING_NATIVE_LIB_DIR
    }
    if (!(Test-Path $PACKAGING_ETC_DIR))
    {
        New-Item -ItemType Directory -Force -Path $PACKAGING_ETC_DIR
    }

    # Copy std-*.dll
    Copy-Item -Path $RUST_STD_LIB.FullName -Destination $PACKAGING_NATIVE_LIB_DIR

    # Copy licenses
    # TODO(ECR-4309): generate rust dependencies licenses file and copy it as well
    Copy-Item -Path "$PROJECT_ROOT\..\LICENSE" -Destination $PACKAGING_ETC_DIR
    Copy-Item -Path "$PROJECT_ROOT\LICENSES-THIRD-PARTY.TXT" -Destination $PACKAGING_ETC_DIR

    # Copy logger fallback configuration file
    Copy-Item -Path "$EJB_RUST_DIR\exonum-java\log4j-fallback.xml" $PACKAGING_ETC_DIR

    # Copy readme
    Copy-Item -Path "$EJB_RUST_DIR\exonum-java\README.md" $PACKAGING_ETC_DIR

    # Package app
    $RUST_LIBRARY_PATH = "$PACKAGING_BASE_DIR\deps\java_bindings.dll"
    $params = @"
package
--activate-profiles package-app
-pl :exonum-java-binding-packaging -am
-DskipTests
-Dbuild.mode=debug
-DskipRustLibBuild
-Drust.libraryPath=$RUST_LIBRARY_PATH
-Drust.compiler.version=$RUST_COMPILER_VERSION
-Dpackaging.exonumJavaName='exonum-java.exe'
"@
    & mvn $params.Split(" ")
}
catch {
    Write-Host "Caught an exception:" -ForegroundColor Red
    Write-Host "Exception Type: $($_.Exception.GetType().FullName)" -ForegroundColor Red
    Write-Host "Exception Message: $($_.Exception.Message)" -ForegroundColor Red
    Exit 1
}
