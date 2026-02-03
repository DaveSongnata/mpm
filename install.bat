@echo off
:: mpm Local Installer for Windows
:: Just double-click this file or run: install.bat

:: Change to the directory where this script is located
cd /d "%~dp0"

echo.
echo  =====================================
echo   mpm - Maven Package Manager
echo   Local Installation
echo  =====================================
echo.
echo  Installing from: %CD%
echo.

:: Check for Java
where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java not found. Please install Java 11 or later.
    echo Download from: https://adoptium.net/
    pause
    exit /b 1
)
echo [OK] Java found

:: Check for Maven
where mvn >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven not found. Please install Maven.
    echo Run: choco install maven
    pause
    exit /b 1
)
echo [OK] Maven found

:: Build the project
echo.
echo Building mpm...
call mvn clean package -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Build failed
    pause
    exit /b 1
)
echo [OK] Build successful

:: Create directories
set "MPM_HOME=%USERPROFILE%\.mpm"
set "MPM_BIN=%MPM_HOME%\bin"

if not exist "%MPM_BIN%" mkdir "%MPM_BIN%"

:: Copy JAR
copy /Y "target\mpm.jar" "%MPM_BIN%\mpm.jar" >nul
echo [OK] Copied mpm.jar to %MPM_BIN%

:: Create wrapper script
echo @echo off> "%MPM_BIN%\mpm.bat"
echo java -jar "%%USERPROFILE%%\.mpm\bin\mpm.jar" %%*>> "%MPM_BIN%\mpm.bat"
echo [OK] Created mpm.bat wrapper

:: Add to PATH using PowerShell (single line command)
echo.
echo Configuring PATH...

powershell -NoProfile -Command "$p=[Environment]::GetEnvironmentVariable('Path','User');if($p-notlike'*.mpm*'){[Environment]::SetEnvironmentVariable('Path',$p+';%MPM_BIN%','User');Write-Host '[OK] Added to PATH' -ForegroundColor Green}else{Write-Host '[OK] Already in PATH' -ForegroundColor Green}"

echo.
echo  =====================================
echo   Installation complete!
echo  =====================================
echo.
echo   IMPORTANT: Close this terminal and open a new one,
echo   then run: mpm help
echo.
pause
