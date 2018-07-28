@echo off

if not defined JAVA_HOME (
	for /f "tokens=2 delims==" %%a in ('java -XshowSettings:properties -version ^2^>^&^1 ^| findstr "java.home"') do set JAVA_HOME=%%a
	set JAVA_HOME=%JAVA_HOME:~1%
)
echo JAVA_HOME=%JAVA_HOME%

for /f "usebackq tokens=* delims=;" %%a in (`cmd /c dir /S "%JAVA_HOME%\jvm.dll" /s /b`) do set JAVA_LIB_DIR=%%a
if "%JAVA_LIB_DIR%" == "" (
    echo "jvm.dll not found. Perhaps you run JRE instead of JDK?"
    exit 1
) else (
    set JAVA_LIB_DIR=%JAVA_LIB_DIR:~,-8%
)
echo JAVA_LIB_DIR=%JAVA_LIB_DIR%

set PATH=%PATH%;%JAVA_LIB_DIR%
set PATH=%PATH%;C:\Users\User\.rustup\toolchains\stable-x86_64-pc-windows-msvc\lib\rustlib\x86_64-pc-windows-msvc\lib\
set PATH=%PATH%;%~dp0exonum-java-binding-core\rust\target\debug
set PATH=%PATH%;%~dp0lib
set PATH=%PATH%;%~dp0
echo PATH=%PATH%

where jvm.dll

rem Workaround for the issue https://github.com/briansmith/ring/issues/648
set CL=/wd5045
echo CL=%CL%

rem set SODIUM_LIB_DIR=C:\devel\pro\exonum-java-binding\lib

rem set RUSTFLAGS=%RUSTFLAGS% -C prefer-dynamic

timeout 3

rem mvn install -DskipTests --activate-profiles ci-build -Drust.compiler.version="stable"
rem mvn install -DskipTests -Drust.compiler.version="stable"
rem mvn clean install -Drust.compiler.version="stable" -Dtest=Ed25519CryptoFunctionTest -DfailIfNoTests=false
mvn clean install -Drust.compiler.version="stable"
