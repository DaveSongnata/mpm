@echo off
:: mpm Local Installer for Windows
:: Just double-click this file or run: install.bat

:: IMPORTANT: Change to the directory where this script is located
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

:: Copy JAR and wrapper
copy /Y "target\mpm.jar" "%MPM_BIN%\mpm.jar" >nul
echo [OK] Copied mpm.jar to %MPM_BIN%

:: Create wrapper script
(
echo @echo off
echo java -jar "%%USERPROFILE%%\.mpm\bin\mpm.jar" %%*
) > "%MPM_BIN%\mpm.bat"
echo [OK] Created mpm.bat wrapper

:: Add to PATH using PowerShell (more reliable than setx)
echo.
echo Configuring PATH...

:: Check if already in PATH
echo %PATH% | find /i ".mpm\bin" >nul
if %ERRORLEVEL% equ 0 (
    echo [OK] Already in PATH
    goto :success
)

:: Use PowerShell to add to User PATH (no character limit, no admin needed)
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$mpmBin = '%MPM_BIN%'; ^
    $currentPath = [Environment]::GetEnvironmentVariable('Path', 'User'); ^
    if ($currentPath -notlike \"*$mpmBin*\") { ^
        $newPath = $currentPath + ';' + $mpmBin; ^
        [Environment]::SetEnvironmentVariable('Path', $newPath, 'User'); ^
        Write-Host '[OK] Added to PATH' -ForegroundColor Green; ^
    } else { ^
        Write-Host '[OK] Already in PATH' -ForegroundColor Green; ^
    }"

if %ERRORLEVEL% neq 0 (
    echo [WARN] Could not add to PATH automatically.
    echo Please add this to your PATH manually: %MPM_BIN%
    echo.
    echo Or run this in PowerShell as Administrator:
    echo   [Environment]::SetEnvironmentVariable('Path', $env:Path + ';%MPM_BIN%', 'User')
)

:success
echo.
echo  =====================================
echo   Installation complete!
echo  =====================================
echo.
echo   IMPORTANT: Close this terminal and open a new one,
echo   then run: mpm help
echo.
echo   Installation location: %MPM_BIN%
echo.
pause
