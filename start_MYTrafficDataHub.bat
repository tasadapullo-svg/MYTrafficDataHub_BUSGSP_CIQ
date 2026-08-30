@echo off
chcp 65001 >nul
setlocal EnableExtensions EnableDelayedExpansion

cd /d "%~dp0"

set "APP_NAME=MYTrafficDataHub"
set "JAR_PATH=%~dp0Application\target\Application-0.0.1-SNAPSHOT.jar"
set "LOG_DIR=%~dp0logs"

if defined MYTRAFFIC_JAVA_HOME (
    set "EFFECTIVE_JAVA_HOME=%MYTRAFFIC_JAVA_HOME%"
) else (
    set "EFFECTIVE_JAVA_HOME=%JAVA_HOME%"
)

if not defined EFFECTIVE_JAVA_HOME (
    if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\java.exe" (
        set "EFFECTIVE_JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
    )
)

echo [%APP_NAME%] JAVA_HOME=%JAVA_HOME%
echo [%APP_NAME%] EFFECTIVE_JAVA_HOME=%EFFECTIVE_JAVA_HOME%

if not defined EFFECTIVE_JAVA_HOME (
    echo [ERROR] MYTrafficDataHub requires Java 17
    exit /b 1
)

set "JAVA_EXE=%EFFECTIVE_JAVA_HOME%\bin\java.exe"
if not exist "%JAVA_EXE%" (
    echo [ERROR] java.exe not found: %JAVA_EXE%
    echo [ERROR] MYTrafficDataHub requires Java 17
    exit /b 1
)

"%JAVA_EXE%" -version
set "JAVA_VERSION_FILE=%TEMP%\%APP_NAME%_java_version.txt"
"%JAVA_EXE%" -version 2>"%JAVA_VERSION_FILE%"
for /f "tokens=3" %%v in ('findstr /i "version" "%JAVA_VERSION_FILE%"') do set "JAVA_VERSION=%%~v"
del "%JAVA_VERSION_FILE%" >nul 2>nul
for /f "tokens=1 delims=." %%m in ("!JAVA_VERSION!") do set "JAVA_MAJOR=%%m"

if not "!JAVA_MAJOR!"=="17" (
    echo [ERROR] MYTrafficDataHub requires Java 17
    echo [ERROR] Detected Java version: !JAVA_VERSION!
    exit /b 1
)

if not exist "%JAR_PATH%" (
    echo [ERROR] JAR not found: %JAR_PATH%
    exit /b 1
)

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

echo [%APP_NAME%] Java Version = !JAVA_VERSION!
echo [%APP_NAME%] JAR = %JAR_PATH%
echo [%APP_NAME%] Working Directory = %CD%
echo [%APP_NAME%] Starting Spring Boot...

"%JAVA_EXE%" -jar "%JAR_PATH%" --spring.profiles.active=longrun
