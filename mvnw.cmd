@ECHO OFF
SETLOCAL

set WRAPPER_DIR=%~dp0.mvn\wrapper
set WRAPPER_PROPERTIES=%WRAPPER_DIR%\maven-wrapper.properties

if not exist "%WRAPPER_PROPERTIES%" (
  echo Could not find %WRAPPER_PROPERTIES%.
  exit /b 1
)

for /f "tokens=1,* delims==" %%A in (%WRAPPER_PROPERTIES%) do (
  if "%%A"=="distributionUrl" set DISTRIBUTION_URL=%%B
)

if "%DISTRIBUTION_URL%"=="" (
  echo distributionUrl is missing in %WRAPPER_PROPERTIES%.
  exit /b 1
)

for %%I in ("%DISTRIBUTION_URL%") do set DIST_FILE=%%~nxI
set DIST_NAME=%DIST_FILE:-bin.zip=%
set CACHE_DIR=%USERPROFILE%\.m2\wrapper\dists\%DIST_NAME%
set ZIP_PATH=%CACHE_DIR%\%DIST_FILE%
set EXTRACT_DIR=%CACHE_DIR%\%DIST_NAME%
set MVN_CMD=%EXTRACT_DIR%\bin\mvn.cmd

if not exist "%MVN_CMD%" (
  if not exist "%CACHE_DIR%" mkdir "%CACHE_DIR%"
  if not exist "%ZIP_PATH%" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -UseBasicParsing -Uri '%DISTRIBUTION_URL%' -OutFile '%ZIP_PATH%'"
    if errorlevel 1 exit /b 1
  )
  if not exist "%EXTRACT_DIR%" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%ZIP_PATH%' -DestinationPath '%CACHE_DIR%' -Force"
    if errorlevel 1 exit /b 1
  )
)

call "%MVN_CMD%" %*
exit /b %ERRORLEVEL%

