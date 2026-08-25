# Context Vault

Context Vault is a Java 21 + JavaFX desktop application for saving and restoring developer work context.

## Product Roadmap

### V1 — Manual Context Saver

Prove the basic idea: developers manually save everything they need to resume work.

- Context name, project name, project path
- Git repository path and branch
- Open files, development commands, notes, tags
- Created/updated timestamps
- Dashboard cards with **Restore**, **Edit**, and **Delete**

### V2 — Git Intelligence

Stop asking for information the computer already knows.

- Detect whether a folder is a Git repository
- Auto-fill repository name, branch, remote URL, current commit, and last commit message
- Show modified, untracked, and staged files
- **Refresh Git** button in the context form

### V3 — Workspace Restoration

Reconstruct the workspace instead of only showing saved information.

- Restore project folder, Git branch, VS Code/Cursor, saved files, terminal, browser URLs, and notes
- Show a restore checklist: Project, Git, VS Code, Files, Terminal, Browser, Notes
- Commands are **not** auto-run — use **Run All** in the restore dialog for safety
- VS Code extension saves/restores open tabs and cursor positions

### V4 — VS Code Extension

The desktop app should not require developers to manually list open files.

- Activity-bar **Context Vault** sidebar with **Save Context**, **Restore Context**, and **My Contexts**
- Detects open tabs, active file, workspace, cursor, selected text, editor groups, terminals, and Git branch
- Writes `.context-vault/latest-context.json` so the desktop app can import open files automatically
- Restores the same files (and recreates missing terminals) from inside VS Code

## Features

- Save project contexts with name, folder, branch, commands, and notes
- Browse and search saved contexts instantly
- Edit or delete existing contexts
- Restore a workspace with a step-by-step checklist
- Persist user settings for editor, terminal, and theme
- SQLite-backed local storage with sample data on first launch
- VS Code / Cursor extension with Save Context / Restore Context / My Contexts

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

When you click **Restore**, Context Vault:

- Opens the project folder in Explorer
- Switches to the saved Git branch when possible
- Opens VS Code or Cursor
- Opens saved files in the editor
- Opens a terminal in the project folder
- Opens saved browser URLs
- Shows your notes and detected commands

Commands are shown in the restore dialog. Click **Run All** when you are ready to execute them.

If VS Code is unavailable, the app falls back to your configured editor from Settings.

## V4 — VS Code Extension

The companion extension in `vscode-extension/` is a first-class Context Vault surface inside the editor:

- Detects open tabs, active file, workspace, cursor, selection, editor groups, terminals, and Git branch
- **Save Context** / **Restore Context** / **My Contexts** in the activity bar
- Writes `.context-vault/latest-context.json` for the desktop app to import open files

### Install the extension

```bash
cd vscode-extension
npm install
npm run compile
npx vsce package --no-dependencies
```

In VS Code or Cursor: **Extensions → … → Install from VSIX…** and choose the generated `.vsix`.

See [`vscode-extension/README.md`](vscode-extension/README.md) for commands and settings.

Session/context data is stored under `.context-vault/` inside each project.

## Notes

- The SQLite database is created automatically in the user application data directory on first run.
- Sample contexts are inserted automatically when the database is first initialized and empty.
