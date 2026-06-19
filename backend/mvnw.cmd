@echo off
setlocal

set "BASE_DIR=%~dp0"
set "MAVEN_VERSION=3.9.9"
set "WRAPPER_DIR=%BASE_DIR%.mvn\wrapper"
set "MAVEN_HOME=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%"
set "MAVEN_BIN=%MAVEN_HOME%\bin\mvn.cmd"

if exist "%MAVEN_BIN%" goto run

where mvn >nul 2>nul
if %ERRORLEVEL% equ 0 (
  mvn %*
  exit /b %ERRORLEVEL%
)

echo Maven was not found in PATH. Downloading Apache Maven %MAVEN_VERSION% for this project...
if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop';" ^
  "$version='%MAVEN_VERSION%';" ^
  "$dir='%WRAPPER_DIR%';" ^
  "$zip=Join-Path $dir ('apache-maven-' + $version + '-bin.zip');" ^
  "$url='https://archive.apache.org/dist/maven/maven-3/' + $version + '/binaries/apache-maven-' + $version + '-bin.zip';" ^
  "if (!(Test-Path $zip)) { Invoke-WebRequest -Uri $url -OutFile $zip };" ^
  "Expand-Archive -Path $zip -DestinationPath $dir -Force"

if not exist "%MAVEN_BIN%" (
  echo Failed to prepare Maven. Please install Maven 3.9+ or check network access.
  exit /b 1
)

:run
"%MAVEN_BIN%" %*
exit /b %ERRORLEVEL%
