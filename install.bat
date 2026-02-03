@echo off
:: mpm Local Installer for Windows
:: Just double-click this file or run: install.bat

echo.
echo  =====================================
echo   mpm - Maven Package Manager
echo   Local Installation
echo  =====================================
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

:: Add to PATH
echo.
echo Checking PATH...
echo %PATH% | find /i "%MPM_BIN%" >nul
if %ERRORLEVEL% neq 0 (
    echo Adding mpm to PATH...
    setx PATH "%PATH%;%MPM_BIN%" >nul 2>&1
    if %ERRORLEVEL% equ 0 (
        echo [OK] Added to PATH
    ) else (
        echo [WARN] Could not add to PATH automatically.
        echo Please add this to your PATH manually: %MPM_BIN%
    )
) else (
    echo [OK] Already in PATH
)

echo.
echo  =====================================
echo   Installation complete!
echo  =====================================
echo.
echo   Close this terminal and open a new one,
echo   then run: mpm help
echo.
pause
