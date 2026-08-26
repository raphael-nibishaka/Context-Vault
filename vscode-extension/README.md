# Context Vault for VS Code

Companion extension for the Context Vault desktop app (V4 + V5).

Detect open tabs, terminals, and Git branch — then use **smart scoring** to suggest the files that belong to your current task.

## Sidebar

```text
Context Vault
────────────────
Save Context
Restore Context

Likely Context
  I think these files belong to your current task.
  PaymentController.ts
  PaymentService.ts
  StripeService.ts
  …
  Save these 5 files as a context?
  [Save Context]

Detected Now
My Contexts
```

## V5 scoring

| Signal | Points |
| --- | --- |
| Active file | +30 |
| Recently modified | +20 |
| Same directory | +15 |
| Git change | +20 |
| Recently opened | +10 |
| Related name/extension | +5 |

Sources also include recent commits and recent terminal activity (when shell integration is available).

## What it detects

| Signal | Source |
| --- | --- |
| Open tabs | Editor tab groups |
| Active file | Active text editor |
| Workspace | VS Code workspace name/path |
| Cursor / selection | Active editor |
| Editor groups | `tabGroups` |
| Open terminals | `window.terminals` |
| Git branch / changes | Git CLI |
| Recent opens | Extension activity tracker |
| Recent terminal commands | Shell integration when available |

## Install

```bash
cd vscode-extension
npm install
npm run compile
npx vsce package --no-dependencies
```

In VS Code / Cursor:

1. Extensions → `...` → **Install from VSIX...**
2. Select `context-vault-1.2.0.vsix`
3. Open the **Context Vault** icon in the activity bar

## Commands

| Command | Description |
| --- | --- |
| `Context Vault: Detect Likely Context` | Refresh smart suggestions |
| `Context Vault: Save Suggested Context` | Save the ranked likely-context files |
| `Context Vault: Save Context` | Capture current workspace state |
| `Context Vault: Restore Context` | Restore a saved context |
| `Context Vault: Show My Contexts` | Focus the sidebar |

## Settings

| Setting | Default | Description |
| --- | --- | --- |
| `contextVault.autoSave` | `true` | Auto-save editor session while you work |
| `contextVault.autoRestore` | `true` | Auto-restore last editor session on open |
| `contextVault.saveDelayMs` | `750` | Debounce for auto-save |
| `contextVault.suggestionLimit` | `8` | Max files in the likely-context list |

## Storage

```text
.context-vault/contexts/<id>.json
.context-vault/contexts/index.json
.context-vault/latest-context.json
.context-vault/editor-session.json
```
