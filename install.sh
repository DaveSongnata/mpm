#!/bin/bash

# mpm Local Installer for Unix (Linux/macOS)
# Run: ./install.sh

set -e

echo ""
echo "  ====================================="
echo "   mpm - Maven Package Manager"
echo "   Local Installation"
echo "  ====================================="
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

# Check for Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}[ERROR]${NC} Java not found. Please install Java 11 or later."
    echo "  macOS:  brew install openjdk@11"
    echo "  Ubuntu: sudo apt install openjdk-11-jdk"
    exit 1
fi
echo -e "${GREEN}[OK]${NC} Java found"

# Check for Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}[ERROR]${NC} Maven not found. Please install Maven."
    echo "  macOS:  brew install maven"
    echo "  Ubuntu: sudo apt install maven"
    exit 1
fi
echo -e "${GREEN}[OK]${NC} Maven found"

# Build the project
echo ""
echo "Building mpm..."
mvn clean package -DskipTests -q
echo -e "${GREEN}[OK]${NC} Build successful"

# Create directories
MPM_HOME="$HOME/.mpm"
MPM_BIN="$MPM_HOME/bin"

mkdir -p "$MPM_BIN"

# Copy JAR
cp -f "target/mpm.jar" "$MPM_BIN/mpm.jar"
echo -e "${GREEN}[OK]${NC} Copied mpm.jar to $MPM_BIN"

# Create wrapper script
cat > "$MPM_BIN/mpm" << 'EOF'
#!/bin/sh
java -jar "$HOME/.mpm/bin/mpm.jar" "$@"
EOF
chmod +x "$MPM_BIN/mpm"
echo -e "${GREEN}[OK]${NC} Created mpm wrapper"

# Add to PATH
echo ""
echo "Checking PATH..."

SHELL_NAME=$(basename "$SHELL")
PROFILE=""

case "$SHELL_NAME" in
    bash)
        [ -f "$HOME/.bashrc" ] && PROFILE="$HOME/.bashrc"
        [ -f "$HOME/.bash_profile" ] && PROFILE="$HOME/.bash_profile"
        ;;
    zsh)
        PROFILE="$HOME/.zshrc"
        ;;
    *)
        PROFILE="$HOME/.profile"
        ;;
esac

if [ -n "$PROFILE" ]; then
    if ! grep -q "\.mpm/bin" "$PROFILE" 2>/dev/null; then
        echo "" >> "$PROFILE"
        echo '# mpm - Maven Package Manager' >> "$PROFILE"
        echo 'export PATH="$HOME/.mpm/bin:$PATH"' >> "$PROFILE"
        echo -e "${GREEN}[OK]${NC} Added to PATH in $PROFILE"
    else
        echo -e "${GREEN}[OK]${NC} Already in PATH"
    fi
fi

echo ""
echo "  ====================================="
echo "   Installation complete!"
echo "  ====================================="
echo ""
echo "   Run: source $PROFILE"
echo "   Then: mpm help"
echo ""
