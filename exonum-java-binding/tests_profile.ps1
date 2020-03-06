Set-StrictMode -Version 1.0
$ErrorActionPreference = "continue"

choco install openjdk -y
choco install maven -y

refreshenv

try
{
    # Find jvm.dll in JAVA_HOME. If JAVA_HOME is not set, we find it by asking java binary.
    # TODO(ECR-4309): error if JAVA_HOME is set, but JRE is used and jvm.dll is unavailable?
    if ($null -eq $env:JAVA_HOME) {
        $args = "-XshowSettings:properties -version"
        $tmpFile = New-TemporaryFile
        $process = Start-Process "java" $args.Split(" ") -NoNewWindow -Wait -PassThru -RedirectStandardError $tmpFile
        if ($process.ExitCode -ne 0) {
            echo "java exited unsuccessfully"
            Exit 1
        }
        $output = Get-Content $tmpFile -Raw
        Remove-Item -Path $tmpFile
        $env:JAVA_HOME = ($output.Split([Environment]::NewLine) | Select-String "java.home").Line.Split("=")[1].Trim()
    }
    echo "JAVA_HOME = $env:JAVA_HOME"
    $env:JAVA_LIB_DIR = Get-ChildItem -Path $env:JAVA_HOME -Filter "jvm.dll" -Recurse | %{$_.Directory}
    echo "JAVA_LIB_DIR = $env:JAVA_LIB_DIR"

    $env:RUST_COMPILER_VERSION = "stable"
    echo "RUST_COMPILER_VERSION = $env:RUST_COMPILER_VERSION"

    # Find std-*.dll (Rust standard library).
    $RUST_SYSROOT = [string] (& rustup run $env:RUST_COMPILER_VERSION rustc --print sysroot)
    $RUST_STD_LIB = (Get-ChildItem -Path $RUST_SYSROOT -Filter "std-*.dll" -Recurse)[0]
    $RUST_LIB_DIR = $RUST_STD_LIB.Directory
    $env:RUST_LIB_DIR = $RUST_LIB_DIR
    echo "RUST_LIB_DIR = $env:RUST_LIB_DIR"

    $PROJECT_ROOT = $PSScriptRoot

    # Use system Path in case of subsequent script runs, append locations of needed native libraries
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") +
            ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")
    $env:Path += ";$env:JAVA_LIB_DIR" # jvm.dll
    $env:Path += ";$env:RUST_LIB_DIR" # std-*.dll
    $env:Path += ";$PROJECT_ROOT\core\rust\target\debug" # java_bindings.dll
    echo "PATH = $env:Path"

    # Fake RUSTFLAGS so maven would be happy
    $env:RUSTFLAGS=" "
}
catch {
    Write-Host "Caught an exception:" -ForegroundColor Red
    Write-Host "Exception Type: $($_.Exception.GetType().FullName)" -ForegroundColor Red
    Write-Host "Exception Message: $($_.Exception.Message)" -ForegroundColor Red
    Exit 1
}
