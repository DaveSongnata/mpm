# mpm Installer for Windows (PowerShell)
# Usage: iwr https://raw.githubusercontent.com/DaveSongnata/mpm/main/installer/install.ps1 -useb | iex

$ErrorActionPreference = "Stop"

$MPM_VERSION = "1.0.0"
$MPM_HOME = "$env:USERPROFILE\.mpm"
$MPM_BIN = "$MPM_HOME\bin"
$JAR_URL = "https://github.com/DaveSongnata/mpm/releases/download/v$MPM_VERSION/mpm.jar"

Write-Host "Installing mpm v$MPM_VERSION..." -ForegroundColor Cyan
Write-Host ""

# Check for Java
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    Write-Host "[OK] Java found: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Java not found. Please install Java 11 or later." -ForegroundColor Red
    Write-Host "Download from: https://adoptium.net/" -ForegroundColor Yellow
    exit 1
}

# Create directories
Write-Host "Creating directories..."
New-Item -ItemType Directory -Force -Path $MPM_HOME | Out-Null
New-Item -ItemType Directory -Force -Path $MPM_BIN | Out-Null

# Download JAR
Write-Host "Downloading mpm.jar..."
try {
    Invoke-WebRequest -Uri $JAR_URL -OutFile "$MPM_BIN\mpm.jar" -UseBasicParsing
    Write-Host "[OK] Downloaded mpm.jar" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Failed to download mpm.jar" -ForegroundColor Red
    Write-Host "URL: $JAR_URL" -ForegroundColor Yellow
    Write-Host "Please download manually and place in: $MPM_BIN\mpm.jar" -ForegroundColor Yellow
    exit 1
}

# Create wrapper script
$wrapperContent = @'
@echo off
setlocal
set "MPM_HOME=%USERPROFILE%\.mpm\bin"
java -jar "%MPM_HOME%\mpm.jar" %*
exit /b %ERRORLEVEL%
'@

Set-Content -Path "$MPM_BIN\mpm.bat" -Value $wrapperContent
Write-Host "[OK] Created mpm.bat wrapper" -ForegroundColor Green

# Add to PATH
$currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($currentPath -notlike "*$MPM_BIN*") {
    Write-Host "Adding mpm to PATH..."
    $newPath = "$currentPath;$MPM_BIN"
    [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
    $env:Path = "$env:Path;$MPM_BIN"
    Write-Host "[OK] Added to PATH" -ForegroundColor Green
} else {
    Write-Host "[OK] Already in PATH" -ForegroundColor Green
}

Write-Host ""
Write-Host "Installation complete!" -ForegroundColor Green
Write-Host ""
Write-Host "To start using mpm, run:" -ForegroundColor Cyan
Write-Host "  mpm help" -ForegroundColor White
Write-Host ""
Write-Host "NOTE: You may need to restart your terminal for PATH changes to take effect." -ForegroundColor Yellow
