# Run all framework tests on Windows platform.
# TODO(ERC-???): split in separate files, like .sh scripts?

Set-StrictMode -Version 1.0
$ErrorActionPreference = "continue"

try
{
    . .\tests_profile.ps1

    $args = "install -Drust.compiler.version=$RUST_COMPILER_VERSION"
    $process = Start-Process mvn $args.Split(" ") -Wait -NoNewWindow -PassThru

    if ($process.ExitCode -ne 0) {
        echo "Maven tests finished unsuccessfully"
        Exit 1
    }

    $args = "+$env:RUST_COMPILER_VERSION test --manifest-path $PROJECT_ROOT\core\rust\integration_tests\Cargo.toml"
    $process = Start-Process cargo $args.Split(" ") -Wait -NoNewWindow -PassThru

    if ($process.ExitCode -ne 0) {
        echo "Native integration tests finished unsuccessfully"
        Exit 1
    }
}
catch {
    Write-Host "Caught an exception:" -ForegroundColor Red
    Write-Host "Exception Type: $($_.Exception.GetType().FullName)" -ForegroundColor Red
    Write-Host "Exception Message: $($_.Exception.Message)" -ForegroundColor Red
    Exit 1
}
