set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_172
rem @echo %JAVA_HOME%
set JAVA_LIB_DIR=%JAVA_HOME%\jre\bin\server
rem @echo %JAVA_LIB_DIR%
set PATH=%PATH%;%JAVA_LIB_DIR%;%~dp0;%~dp0lib;C:\devel\pro\exonum-java-binding\exonum-java-binding-core\rust\target\debug
set PATH=%PATH%;C:\Users\User\.rustup\toolchains\stable-x86_64-pc-windows-msvc\lib\rustlib\x86_64-pc-windows-msvc\lib\
set PATH=%PATH%;C:\devel\pro\exonum-java-binding\lib
rem @echo %PATH%

rem Workaround for the issue https://github.com/briansmith/ring/issues/648
set CL=/wd5045

rem set SODIUM_LIB_DIR=C:\devel\pro\exonum-java-binding\lib

where jvm.dll

set RUSTFLAGS=%RUSTFLAGS% -C prefer-dynamic

timeout 3

rem mvn install -DskipTests --activate-profile`s ci-build -Drust.compiler.version="stable"
rem mvn install -DskipTests -Drust.compiler.version="stable"
mvn clean install -Drust.compiler.version="stable"
