@echo off

rem Ask Java where its home
if not defined JAVA_HOME (
        for /f "tokens=2 delims==" %%a in ('java -XshowSettings:properties -version ^2^>^&^1 ^| findstr "java.home"') do set JAVA_HOME=%%a
)

rem Trim left spaces
for /f "tokens=* delims= " %%a in ("%JAVA_HOME%") do set JAVA_HOME=%%a
echo JAVA_HOME=%JAVA_HOME%

rem Find the directory containing jvm.dll in JAVA_HOME
for /f "usebackq tokens=* delims=;" %%a in (`cmd /c dir /S "%JAVA_HOME%\jvm.dll" /s /b`) do set JAVA_LIB_DIR=%%a
if "%JAVA_LIB_DIR%" == "" (
    echo "jvm.dll not found. Perhaps you run JRE instead of JDK?"
    exit /b 1
) else (
    set JAVA_LIB_DIR=%JAVA_LIB_DIR:~,-8%
)
echo JAVA_LIB_DIR=%JAVA_LIB_DIR%

set RUST_COMPILER_VERSION="stable"
rem Find the directory containing stdlib.dll
for /f "tokens=2 delims==" %%a in ('rustup run %RUST_COMPILER_VERSION% rustc --print sysroot') do set RUST_LIB_DIR=%%a

set PATH=%PATH%;%JAVA_LIB_DIR%
set PATH=%PATH%;%RUST_LIB_DRIR%\lib
set PATH=%PATH%;%~dp0exonum-java-binding-core\rust\target\debug
set PATH=%PATH%;%~dp0lib
set PATH=%PATH%;%~dp0
echo PATH=%PATH%

where jvm.dll

rem Workaround for the issue https://github.com/briansmith/ring/issues/648
set CL=/wd5045
echo CL=%CL%

rem set RUSTFLAGS=%RUSTFLAGS% -C prefer-dynamic

timeout 3

rem mvn clean install -Drust.compiler.version="%RUST_COMPILER_VERSION%" -Dtest=Ed25519CryptoFunctionTest -DfailIfNoTests=false
mvn clean install --activate-profiles ci-build -Drust.compiler.version="%RUST_COMPILER_VERSION%"
rem mvn clean install -Drust.compiler.version="%RUST_COMPILER_VERSION%"
