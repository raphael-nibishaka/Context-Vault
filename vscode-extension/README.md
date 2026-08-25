# Context Vault for VS Code

Companion extension for the Context Vault desktop app (V4).

Detect open tabs, active file, workspace, cursor position, selected text, editor groups, terminals, and Git branch — then save/restore named contexts from a sidebar inside VS Code.

## Sidebar

```text
Context Vault
────────────────
Save Context
Restore Context

Detected Now
  Workspace · AgriSense
  Branch · feature/authentication
  Active · AuthMiddleware.ts
  Open files · AuthService.ts, JwtService.ts, ...

My Contexts
  AgriSense · feature/authentication
  [Restore] [Delete]
```

## What it detects

| Signal | Source |
| --- | --- |
| Open tabs | Editor tab groups |
| Active file | Active text editor |
| Workspace | VS Code workspace name/path |
| Cursor position | Active selection |
| Selected text | Current selection |
| Editor groups | `tabGroups` |
| Open terminals | `window.terminals` |
| Git branch | `git branch --show-current` |

## Save Context payload example

```json
{
  "workspace": "AgriSense",
  "activeFile": "AuthMiddleware.ts",
  "openFiles": [
    "AuthService.ts",
    "JwtService.ts",
    "UserController.ts",
    "AuthMiddleware.ts"
  ],
  "gitBranch": "feature/authentication"
}
```

Saved under:

```text
.context-vault/contexts/<id>.json
.context-vault/contexts/index.json
.context-vault/latest-context.json   ← bridge for the desktop app
.context-vault/editor-session.json   ← auto tab/cursor session
```

When you browse the same project folder in the desktop app, it can auto-fill open files, branch, and name from `latest-context.json`.

## Install

```bash
cd vscode-extension
npm install
npm run compile
npx vsce package --no-dependencies
```

In VS Code / Cursor:

1. Extensions → `...` → **Install from VSIX...**
2. Select `context-vault-1.1.0.vsix`
3. Open the **Context Vault** icon in the activity bar

## Commands

| Command | Description |
| --- | --- |
| `Context Vault: Save Context` | Capture workspace state into a named context |
| `Context Vault: Restore Context` | Restore the latest / chosen context |
| `Context Vault: Show My Contexts` | Focus the sidebar |
| `Context Vault: Save Editor Session` | Auto-session snapshot only |
| `Context Vault: Restore Editor Session` | Restore auto-session tabs/cursors |

## Settings

| Setting | Default | Description |
| --- | --- | --- |
| `contextVault.autoSave` | `true` | Auto-save editor session while you work |
| `contextVault.autoRestore` | `true` | Auto-restore last editor session on open |
| `contextVault.saveDelayMs` | `750` | Debounce for auto-save |

## Development

```bash
cd vscode-extension
npm install
npm run watch
```

Press **F5** with the repo open to launch an Extension Development Host.
