# Context Vault

Context Vault is a Java 21 + JavaFX desktop application for saving and restoring developer work context.

## Features

- Save project contexts with name, folder, branch, commands, and notes
- Browse and search saved contexts instantly
- Edit or delete existing contexts
- Restore a context by opening the folder, launching the configured editor, opening a terminal, and showing saved commands and notes
- Persist user settings for editor, terminal, and theme
- SQLite-backed local storage with sample data on first launch
- **V3:** VS Code / Cursor extension that detects open tabs, saves cursor positions, and restores them when the project reopens

## Tech Stack

- Java 21
- JavaFX
- Maven
- SQLite with JDBC
- Jackson
- Ikonli
- SLF4J + Logback

## Run

### Prerequisites

- Java 21
- Maven 3.9+

### Launch

```bash
mvn javafx:run
```

### Package / Compile

```bash
mvn clean compile
```

## Project Structure

```text
src/main/java
├── app
├── config
├── controllers
├── database
├── models
├── repository
├── services
├── utils
└── viewmodels

src/main/resources
├── css
├── fxml
├── icons
└── images

vscode-extension
├── src
└── README.md
```

## Restore Behavior

When you click **Open**, Context Vault automatically:

- Detects and validates the saved project folder
- Detects the current Git branch and switches to the saved branch when possible
- Opens the project folder in Explorer
- Opens the project in VS Code
- Opens your configured terminal in the project folder
- Starts saved commands in that terminal

If VS Code is unavailable, the app falls back to your configured editor from Settings.

## V3 — VS Code Extension

The companion extension in `vscode-extension/` makes restore smoother by remembering editor state:

- Detects open tabs across editor groups
- Saves cursor positions, selections, and visible ranges
- Restores them automatically when the project is reopened

### Install the extension

```bash
cd vscode-extension
npm install
npm run compile
npx vsce package --no-dependencies
```

In VS Code or Cursor: **Extensions → … → Install from VSIX…** and choose the generated `.vsix`.

See [`vscode-extension/README.md`](vscode-extension/README.md) for commands and settings.

Session data is stored at `.context-vault/editor-session.json` inside each project.

## Notes

- The SQLite database is created automatically in the user application data directory on first run.
- Sample contexts are inserted automatically when the database is first initialized and empty.
