#!/bin/bash

# mpm Installer for Unix (Linux/macOS)
# Usage: curl -fsSL https://raw.githubusercontent.com/DaveSongnata/mpm/main/installer/install.sh | bash

set -e

MPM_VERSION="1.0.0"
MPM_HOME="$HOME/.mpm"
MPM_BIN="$MPM_HOME/bin"
JAR_URL="https://github.com/DaveSongnata/mpm/releases/download/v$MPM_VERSION/mpm.jar"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${CYAN}Installing mpm v$MPM_VERSION...${NC}"
echo ""

# Check for Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    echo -e "${GREEN}[OK]${NC} Java found: $JAVA_VERSION"
else
    echo -e "${RED}[ERROR]${NC} Java not found. Please install Java 11 or later."
    echo -e "${YELLOW}Install via:${NC}"
    echo "  macOS:  brew install openjdk@11"
    echo "  Ubuntu: sudo apt install openjdk-11-jdk"
    echo "  Or download from: https://adoptium.net/"
    exit 1
fi

# Create directories
echo "Creating directories..."
mkdir -p "$MPM_HOME"
mkdir -p "$MPM_BIN"

# Download JAR
echo "Downloading mpm.jar..."
if command -v curl &> /dev/null; then
    curl -fsSL "$JAR_URL" -o "$MPM_BIN/mpm.jar"
elif command -v wget &> /dev/null; then
    wget -q "$JAR_URL" -O "$MPM_BIN/mpm.jar"
else
    echo -e "${RED}[ERROR]${NC} Neither curl nor wget found"
    exit 1
fi
echo -e "${GREEN}[OK]${NC} Downloaded mpm.jar"

# Create wrapper script
cat > "$MPM_BIN/mpm" << 'EOF'
#!/bin/sh
MPM_HOME="$HOME/.mpm/bin"
exec java -jar "$MPM_HOME/mpm.jar" "$@"
EOF
chmod +x "$MPM_BIN/mpm"
echo -e "${GREEN}[OK]${NC} Created mpm wrapper"

# Detect shell and add to PATH
SHELL_NAME=$(basename "$SHELL")
PROFILE=""

case "$SHELL_NAME" in
    bash)
        if [ -f "$HOME/.bashrc" ]; then
            PROFILE="$HOME/.bashrc"
        elif [ -f "$HOME/.bash_profile" ]; then
            PROFILE="$HOME/.bash_profile"
        fi
        ;;
    zsh)
        PROFILE="$HOME/.zshrc"
        ;;
    fish)
        PROFILE="$HOME/.config/fish/config.fish"
        ;;
    *)
        PROFILE="$HOME/.profile"
        ;;
esac

# Add to PATH if not already present
if [ -n "$PROFILE" ]; then
    if ! grep -q "\.mpm/bin" "$PROFILE" 2>/dev/null; then
        echo "" >> "$PROFILE"
        echo "# mpm - Maven Package Manager" >> "$PROFILE"
        echo 'export PATH="$HOME/.mpm/bin:$PATH"' >> "$PROFILE"
        echo -e "${GREEN}[OK]${NC} Added to PATH in $PROFILE"
    else
        echo -e "${GREEN}[OK]${NC} Already in PATH"
    fi
fi

# Add to current session
export PATH="$MPM_BIN:$PATH"

echo ""
echo -e "${GREEN}Installation complete!${NC}"
echo ""
echo -e "${CYAN}To start using mpm, run:${NC}"
echo "  mpm help"
echo ""
echo -e "${YELLOW}NOTE: You may need to restart your terminal or run:${NC}"
echo "  source $PROFILE"
