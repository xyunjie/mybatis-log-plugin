# MyBatis SQL Restore

MyBatis SQL Restore is an IntelliJ IDEA plugin that converts standard MyBatis log output into executable SQL and displays the results in a dedicated tool window.

## Features

- Automatically listens to the current project's Run/Debug output and captures MyBatis SQL logs
- Parses standard `Preparing:` and `Parameters:` log pairs into executable SQL
- Supports manual paste parsing for full MyBatis log blocks
- Provides keyword search across source, restored SQL, parameterized SQL, and parameters
- Shows structured details for each captured record:
  - parsed SQL
  - parameterized SQL
  - parameters
- Supports English and Chinese by following the IDE language setting

## Supported Log Format

The plugin currently targets the standard MyBatis log format:

```text
Preparing: select * from user where id = ? and name = ?
Parameters: 1(Integer), Alice(String)
```

Parsed result:

```sql
select * from user where id = 1 and name = 'Alice'
```

## Installation

### Install from ZIP

1. Build the plugin package:

```bash
gradle buildPlugin
```

2. Open IntelliJ IDEA.
3. Go to **Settings / Preferences** → **Plugins**.
4. Click the gear icon → **Install Plugin from Disk...**.
5. Select the generated ZIP file from:

```text
build/distributions/mybatis-log-plugin-0.1.0.zip
```

6. Restart IDEA.

## Usage

After installation, open the tool window:

- **View** → **Tool Windows** → **Mybatis Log**

The plugin provides two tabs.

### Auto

Use this tab to inspect MyBatis logs captured from the current project's Run/Debug console.

- Left side: captured SQL records
- Right side:
  - **Parsed SQL** tab
  - **Raw Content** tab with:
    - parameterized SQL
    - parameters
- Top toolbar:
  - search box
  - **Copy SQL**
  - **Clear Auto SQL**

### Manual

Use this tab to paste raw MyBatis log output and parse it manually.

- Left side: original MyBatis output
- Right side: parsed SQL result
- Actions:
  - **Parse**
  - **Copy Result**
  - **Clear Input**

## Development

### Environment

- JDK 21
- IntelliJ IDEA installed locally
- Gradle 9+

Current project settings:

- IntelliJ Platform Gradle Plugin: `2.13.1`
- Kotlin: `2.1.20`
- Since build: `243`
- Until build: `253.*`

### Run tests

```bash
gradle test
```

### Run the plugin in a sandbox IDE

```bash
gradle runIde
```

### Build the plugin package

```bash
gradle buildPlugin
```

## Notes

- The plugin currently supports standard MyBatis `Preparing:` / `Parameters:` log output.
- It only listens to the current project's execution output.
- Searchable options generation is disabled in the build because this plugin does not provide a Settings page and local packaging was blocked by bundled Ultimate-only plugins during that task.

## Project Structure

```text
src/main/kotlin/com/github/accepted/mybatislogplugin/
├── listener/    # Run/Debug output listener
├── model/       # Log entry models
├── parser/      # MyBatis log parsing and parameter formatting
├── service/     # Project-level state and filtering
├── ui/          # Tool window UI
└── MyBatisLogBundle.kt
```

## License

Please add a license if you plan to publish this project publicly.
