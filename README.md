# mpm - Maven Package Manager

> npm-like CLI for Maven dependencies. Stop editing XML manually.

[![Java](https://img.shields.io/badge/Java-11%2B-orange)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

```bash
# Instead of editing pom.xml manually...
mpm install lombok
```

## Why?

Adding a Maven dependency requires:
1. Google the artifact
2. Find the XML snippet
3. Copy-paste into pom.xml
4. Reload Maven in IDE

With **mpm**, it's just one command. Like npm, but for Maven.

## Installation

### From Source (Recommended)

```bash
git clone https://github.com/DaveSongnata/mpm.git
cd mpm
```

**Windows:** Double-click `install.bat` or run:
```cmd
install.bat
```

**Linux / macOS:**
```bash
chmod +x install.sh
./install.sh
```

Then open a new terminal and run `mpm help`.

### One-liner Install (from releases)

**Windows (PowerShell):**
```powershell
iwr https://raw.githubusercontent.com/DaveSongnata/mpm/main/installer/install.ps1 -useb | iex
```

**Linux / macOS:**
```bash
curl -fsSL https://raw.githubusercontent.com/DaveSongnata/mpm/main/installer/install.sh | bash
```

## Requirements

- Java 11 or later
- Maven (for dependency resolution)

## Usage

### Initialize a new project

```bash
mpm init
```

Creates a basic `pom.xml` in the current directory.

```bash
mpm init --yes  # Use defaults, no prompts
mpm init --groupId com.mycompany --artifactId myapp
```

### Install dependencies

```bash
# Install latest version
mpm install lombok

# Install specific version
mpm install jackson-databind@2.15.2

# Install with full coordinates
mpm install com.google.guava:guava

# Install with specific scope
mpm install junit --scope test
mpm install servlet-api --scope provided
```

### Search for packages

```bash
mpm search spring-boot

# Limit results
mpm search kafka --limit 5
```

Output:
```
Found 10 artifact(s):

 1. org.springframework.boot:spring-boot
    Latest: 3.2.0 (156 versions)
    Install: mpm install org.springframework.boot:spring-boot

 2. org.springframework.boot:spring-boot-starter
    Latest: 3.2.0 (142 versions)
...
```

### List dependencies

```bash
mpm list
```

Output:
```
Dependencies (3):

  Compile:
    org.projectlombok:lombok@1.18.30
    com.fasterxml.jackson.core:jackson-databind@2.15.2

  Test:
    org.junit.jupiter:junit-jupiter@5.10.0
```

### Remove dependencies

```bash
mpm remove lombok

# If multiple artifacts match, use full coordinates
mpm remove com.fasterxml.jackson.core:jackson-databind
```

## Command Aliases

| Alias | Command |
|-------|---------|
| `i`, `add` | `install` |
| `rm`, `uninstall` | `remove` |
| `ls` | `list` |
| `s`, `find` | `search` |

```bash
mpm i lombok      # Same as: mpm install lombok
mpm rm junit      # Same as: mpm remove junit
```

## Scopes

| Scope | Description |
|-------|-------------|
| `compile` | Default. Available in all classpaths. |
| `test` | Only for test compilation and execution. |
| `provided` | Expected to be provided by JDK or container. |
| `runtime` | Not needed for compilation, only execution. |

```bash
mpm install lombok                    # compile (default)
mpm install junit --scope test        # test
mpm install servlet-api --scope provided
mpm install mysql-connector --scope runtime
```

## How it works

1. **Search**: Queries [Maven Central API](https://search.maven.org/) for artifact coordinates
2. **Edit**: Adds `<dependency>` to your `pom.xml` using XML DOM (preserves formatting)
3. **Resolve**: Runs `mvn dependency:resolve` to download JARs

## Building from source

```bash
git clone https://github.com/DaveSongnata/mpm.git
cd mpm
mvn clean package
java -jar target/mpm.jar help
```

## Roadmap

- [ ] `mpm update` - Update dependencies to latest versions
- [ ] `mpm outdated` - Show outdated dependencies
- [ ] `mpm audit` - Security vulnerability check
- [ ] Local cache for faster searches
- [ ] Support for private repositories
- [ ] GraalVM native image for faster startup

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

MIT License - see [LICENSE](LICENSE) for details.

---

**mpm** - Because life's too short to edit XML.
