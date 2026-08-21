# Context Vault for VS Code

Companion extension for the Context Vault desktop app. It keeps your editor flow intact by saving and restoring open tabs and cursor positions.

## What it does

- Detects open editor tabs across all groups
- Saves cursor position, selection, and visible range for each tab
- Writes session state to `.context-vault/editor-session.json` in the project
- Restores tabs and cursors automatically when the project is reopened
- Works with Context Vault desktop restore (open project in VS Code / Cursor)

## Install

### From this repository

```bash
cd vscode-extension
npm install
npm run compile
npx vsce package --no-dependencies
```

Then in VS Code / Cursor:

1. Extensions view → `...` → **Install from VSIX...**
2. Select the generated `.vsix` file

### Development

```bash
cd vscode-extension
npm install
npm run watch
```

Press **F5** in VS Code with this folder open to launch an Extension Development Host.

## Commands

| Command | Description |
| --- | --- |
| `Context Vault: Save Editor Session` | Save open tabs and cursors now |
| `Context Vault: Restore Editor Session` | Restore the last saved session |
| `Context Vault: Clear Saved Session` | Delete the saved session file |

## Settings

| Setting | Default | Description |
| --- | --- | --- |
| `contextVault.autoSave` | `true` | Auto-save while you work |
| `contextVault.autoRestore` | `true` | Auto-restore on project open |
| `contextVault.saveDelayMs` | `750` | Debounce delay for auto-save |

## Session file

Saved relative to the workspace root:

```text
.context-vault/editor-session.json
```

Add `.context-vault/` to your project `.gitignore` if you do not want session files committed.

## Smooth Context Vault flow

1. Install this extension in VS Code or Cursor
2. Work normally — tabs and cursors are saved automatically
3. Use Context Vault desktop **Open** to restore branch, folder, editor, and commands
4. When the project opens, this extension restores your tabs and cursor positions
