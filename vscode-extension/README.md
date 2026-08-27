# Context Vault for VS Code

Companion extension for the Context Vault desktop app (V4–V6).

Detect open tabs, terminals, and Git branch — use **smart scoring** to suggest task files — then generate optional **AI / heuristic** summaries for save and welcome-back handoff.

## Sidebar

```text
Context Vault
────────────────
Save Context
Restore Context

Welcome Back
  You were working on JWT Authentication
  Last activity: 2 days ago
  Suggested next step: Run auth integration tests

Likely Context
  I think these files belong to your current task.
  …
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

## V6 intelligence (optional AI)

When saving a context, Context Vault generates:

- Context summary
- Current work bullets
- Likely next step
- Task / commit / handoff summaries

AI is **optional**. Without an API key, heuristic summaries are still generated from branch + files + notes.

### Enable AI

1. Command Palette → `Context Vault: Set AI API Key`
2. Settings → `contextVault.enableAi = true`
3. Optional: `contextVault.aiEndpoint`, `contextVault.aiModel`

## Install

```bash
cd vscode-extension
npm install
npm run compile
npx vsce package --no-dependencies
```

Install `context-vault-1.3.0.vsix` via **Extensions → Install from VSIX…**

## Commands

| Command | Description |
| --- | --- |
| `Context Vault: Detect Likely Context` | Refresh smart suggestions |
| `Context Vault: Save Suggested Context` | Save ranked likely-context files |
| `Context Vault: Save Context` | Capture + summarize current workspace |
| `Context Vault: Restore Context` | Restore a saved context |
| `Context Vault: Set AI API Key` | Store OpenAI-compatible API key securely |

## Settings

| Setting | Default | Description |
| --- | --- | --- |
| `contextVault.autoSave` | `true` | Auto-save editor session |
| `contextVault.autoRestore` | `true` | Auto-restore last editor session |
| `contextVault.suggestionLimit` | `8` | Max likely-context files |
| `contextVault.enableAi` | `false` | Use AI provider when available |
| `contextVault.aiEndpoint` | OpenAI chat completions | OpenAI-compatible endpoint |
| `contextVault.aiModel` | `gpt-4o-mini` | Model name |

## Storage

```text
.context-vault/contexts/<id>.json
.context-vault/contexts/index.json
.context-vault/latest-context.json
.context-vault/editor-session.json
```
