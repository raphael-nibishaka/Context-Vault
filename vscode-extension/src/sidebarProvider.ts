import * as vscode from "vscode";
import type {
  ContextSummary,
  LikelyContextSuggestion,
  VaultContext,
  WelcomeBackMessage,
} from "./types";

export class ContextVaultViewProvider implements vscode.WebviewViewProvider {
  public static readonly viewType = "contextVault.sidebar";

  private view?: vscode.WebviewView;
  private contexts: ContextSummary[] = [];
  private preview?: Partial<VaultContext>;
  private suggestion?: LikelyContextSuggestion;
  private welcome?: WelcomeBackMessage;

  constructor(
    private readonly extensionUri: vscode.Uri,
    private readonly handlers: {
      onSave: () => Promise<void>;
      onSaveSuggestion: () => Promise<void>;
      onRestoreLatest: () => Promise<void>;
      onRestore: (id: string) => Promise<void>;
      onDelete: (id: string) => Promise<void>;
      onRefresh: () => Promise<void>;
    }
  ) {}

  resolveWebviewView(
    webviewView: vscode.WebviewView,
    _context: vscode.WebviewViewResolveContext,
    _token: vscode.CancellationToken
  ): void {
    this.view = webviewView;
    webviewView.webview.options = {
      enableScripts: true,
      localResourceRoots: [this.extensionUri],
    };
    webviewView.webview.html = this.getHtml();

    webviewView.webview.onDidReceiveMessage(async (message) => {
      switch (message.type) {
        case "save":
          await this.handlers.onSave();
          break;
        case "saveSuggestion":
          await this.handlers.onSaveSuggestion();
          break;
        case "restoreLatest":
          await this.handlers.onRestoreLatest();
          break;
        case "restore":
          await this.handlers.onRestore(message.id);
          break;
        case "delete":
          await this.handlers.onDelete(message.id);
          break;
        case "refresh":
          await this.handlers.onRefresh();
          break;
        default:
          break;
      }
    });

    void this.handlers.onRefresh();
  }

  setContexts(contexts: ContextSummary[]): void {
    this.contexts = contexts;
    this.postState();
  }

  setPreview(preview: Partial<VaultContext> | undefined): void {
    this.preview = preview;
    this.postState();
  }

  setSuggestion(suggestion: LikelyContextSuggestion | undefined): void {
    this.suggestion = suggestion;
    this.postState();
  }

  setWelcome(welcome: WelcomeBackMessage | undefined): void {
    this.welcome = welcome;
    this.postState();
  }

  private postState(): void {
    void this.view?.webview.postMessage({
      type: "state",
      contexts: this.contexts,
      preview: this.preview,
      suggestion: this.suggestion,
      welcome: this.welcome,
    });
  }

  private getHtml(): string {
    return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <style>
    :root { color-scheme: light dark; --gap: 10px; --radius: 8px; }
    body {
      font-family: var(--vscode-font-family);
      font-size: var(--vscode-font-size);
      color: var(--vscode-foreground);
      background: transparent;
      margin: 0;
      padding: 12px;
    }
    h1 {
      font-size: 13px; font-weight: 700; margin: 0 0 12px;
      letter-spacing: 0.04em; text-transform: uppercase; opacity: 0.85;
    }
    .stack { display: flex; flex-direction: column; gap: var(--gap); }
    button {
      border: 1px solid var(--vscode-button-border, transparent);
      border-radius: var(--radius); padding: 8px 10px; cursor: pointer;
      font: inherit; width: 100%; text-align: left;
    }
    .primary { background: var(--vscode-button-background); color: var(--vscode-button-foreground); }
    .primary:hover { background: var(--vscode-button-hoverBackground); }
    .secondary { background: var(--vscode-button-secondaryBackground); color: var(--vscode-button-secondaryForeground); }
    .secondary:hover { background: var(--vscode-button-secondaryHoverBackground); }
    .ghost {
      background: transparent; color: var(--vscode-foreground);
      border-color: var(--vscode-input-border, rgba(127,127,127,0.35));
    }
    .card {
      border: 1px solid var(--vscode-input-border, rgba(127,127,127,0.35));
      border-radius: var(--radius); padding: 10px;
      background: var(--vscode-editor-background);
    }
    .muted { opacity: 0.72; font-size: 12px; }
    .title { font-weight: 600; margin-bottom: 4px; }
    .row { display: flex; gap: 6px; }
    .row button { width: auto; flex: 1; }
    .preview-line { margin: 2px 0; word-break: break-word; }
    .empty { opacity: 0.65; font-size: 12px; padding: 8px 0; }
    .section-label {
      margin-top: 8px; margin-bottom: 4px; font-size: 11px; font-weight: 700;
      letter-spacing: 0.05em; text-transform: uppercase; opacity: 0.7;
    }
    .danger {
      background: transparent; color: var(--vscode-errorForeground);
      border-color: color-mix(in srgb, var(--vscode-errorForeground) 35%, transparent);
    }
    .file-list { margin: 8px 0 0; padding-left: 16px; }
    .file-list li { margin: 3px 0; word-break: break-word; }
    .score { opacity: 0.65; font-size: 11px; }
    .prompt { margin-top: 8px; font-weight: 600; }
    .welcome { border-color: color-mix(in srgb, var(--vscode-focusBorder) 45%, transparent); }
  </style>
</head>
<body>
  <h1>Context Vault</h1>
  <div class="stack">
    <button class="primary" id="saveBtn">Save Context</button>
    <button class="secondary" id="restoreBtn">Restore Context</button>
    <button class="ghost" id="refreshBtn">Refresh</button>
  </div>

  <div class="section-label">Welcome Back</div>
  <div class="card welcome" id="welcomeCard">
    <div class="empty">Save a context to unlock welcome-back intelligence.</div>
  </div>

  <div class="section-label">Likely Context</div>
  <div class="card" id="suggestionCard">
    <div class="empty">Analyzing your current task…</div>
  </div>

  <div class="section-label">Detected Now</div>
  <div class="card" id="previewCard">
    <div class="empty">Open a workspace to detect tabs, terminals, and Git branch.</div>
  </div>

  <div class="section-label">My Contexts</div>
  <div class="stack" id="contextsList">
    <div class="empty">No saved contexts yet.</div>
  </div>

  <script>
    const vscode = acquireVsCodeApi();
    const previewCard = document.getElementById('previewCard');
    const contextsList = document.getElementById('contextsList');
    const suggestionCard = document.getElementById('suggestionCard');
    const welcomeCard = document.getElementById('welcomeCard');

    document.getElementById('saveBtn').addEventListener('click', () => vscode.postMessage({ type: 'save' }));
    document.getElementById('restoreBtn').addEventListener('click', () => vscode.postMessage({ type: 'restoreLatest' }));
    document.getElementById('refreshBtn').addEventListener('click', () => vscode.postMessage({ type: 'refresh' }));

    function escapeHtml(value) {
      return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;');
    }

    function renderWelcome(welcome) {
      if (!welcome) {
        welcomeCard.innerHTML = '<div class="empty">Save a context to unlock welcome-back intelligence.</div>';
        return;
      }
      welcomeCard.innerHTML = \`
        <div class="title">\${escapeHtml(welcome.title)}</div>
        <div class="preview-line">You were working on <strong>\${escapeHtml(welcome.contextName)}</strong></div>
        <div class="muted">Last activity: \${escapeHtml(welcome.lastActivity)}</div>
        <div class="muted">You had \${welcome.modifiedFileCount} file(s) in context.</div>
        <div class="preview-line" style="margin-top:8px"><strong>Last note</strong><br/>\${escapeHtml(welcome.lastNote)}</div>
        <div class="preview-line" style="margin-top:8px"><strong>Suggested next step</strong><br/>\${escapeHtml(welcome.nextStep)}</div>
        <div class="muted" style="margin-top:8px">\${escapeHtml(welcome.summary)}</div>
      \`;
    }

    function renderSuggestion(suggestion) {
      if (!suggestion) {
        suggestionCard.innerHTML = '<div class="empty">Open files or make Git changes to get smart suggestions.</div>';
        return;
      }
      if (!suggestion.files || suggestion.files.length === 0) {
        suggestionCard.innerHTML = \`
          <div class="title">I think these files belong to your current task.</div>
          <div class="muted">Branch · \${escapeHtml(suggestion.branch || 'n/a')}</div>
          <div class="empty">\${escapeHtml(suggestion.prompt)}</div>
        \`;
        return;
      }
      const files = suggestion.files.map((file) => \`
        <li>
          <strong>\${escapeHtml(file.fileName)}</strong>
          <span class="score">· \${file.score}</span>
          <div class="muted">\${escapeHtml((file.reasons || []).join(', '))}</div>
        </li>
      \`).join('');
      suggestionCard.innerHTML = \`
        <div class="title">I think these files belong to your current task.</div>
        <div class="muted">Branch · \${escapeHtml(suggestion.branch || 'n/a')}</div>
        <ul class="file-list">\${files}</ul>
        <div class="prompt">\${escapeHtml(suggestion.prompt)}</div>
        <button class="primary" id="saveSuggestionBtn" style="margin-top:8px">Save Context</button>
      \`;
      document.getElementById('saveSuggestionBtn')?.addEventListener('click', () => {
        vscode.postMessage({ type: 'saveSuggestion' });
      });
    }

    function renderPreview(preview) {
      if (!preview || !preview.workspace) {
        previewCard.innerHTML = '<div class="empty">Open a workspace to detect tabs, terminals, and Git branch.</div>';
        return;
      }
      const files = (preview.openFiles || []).slice(0, 6).map(escapeHtml).join(', ') || 'None';
      const intelligence = preview.intelligence;
      previewCard.innerHTML = \`
        <div class="preview-line"><strong>Workspace</strong> · \${escapeHtml(preview.workspace)}</div>
        <div class="preview-line"><strong>Branch</strong> · \${escapeHtml(preview.gitBranch || 'n/a')}</div>
        <div class="preview-line"><strong>Active</strong> · \${escapeHtml(preview.activeFile || 'n/a')}</div>
        <div class="preview-line"><strong>Open files</strong> · \${files}</div>
        \${intelligence ? \`
          <div class="section-label" style="margin-top:10px">Context Summary</div>
          <div class="preview-line">\${escapeHtml(intelligence.summary)}</div>
          <div class="muted">Next: \${escapeHtml(intelligence.nextStep)}</div>
          <div class="muted">Source: \${escapeHtml(intelligence.source)}</div>
        \` : ''}
      \`;
    }

    function renderContexts(contexts) {
      if (!contexts || contexts.length === 0) {
        contextsList.innerHTML = '<div class="empty">No saved contexts yet.</div>';
        return;
      }
      contextsList.innerHTML = contexts.map((context) => \`
        <div class="card" data-id="\${escapeHtml(context.id)}">
          <div class="title">\${escapeHtml(context.name)}</div>
          <div class="muted">\${escapeHtml(context.gitBranch || 'no branch')} · \${context.openFileCount} files</div>
          \${context.summary ? \`<div class="muted">\${escapeHtml(context.summary)}</div>\` : ''}
          \${context.nextStep ? \`<div class="muted">Next: \${escapeHtml(context.nextStep)}</div>\` : ''}
          <div class="muted">\${new Date(context.savedAt).toLocaleString()}</div>
          <div class="row" style="margin-top:8px">
            <button class="secondary restore-one">Restore</button>
            <button class="danger delete-one">Delete</button>
          </div>
        </div>
      \`).join('');

      contextsList.querySelectorAll('.restore-one').forEach((button) => {
        button.addEventListener('click', (event) => {
          const id = event.target.closest('[data-id]').getAttribute('data-id');
          vscode.postMessage({ type: 'restore', id });
        });
      });
      contextsList.querySelectorAll('.delete-one').forEach((button) => {
        button.addEventListener('click', (event) => {
          const id = event.target.closest('[data-id]').getAttribute('data-id');
          vscode.postMessage({ type: 'delete', id });
        });
      });
    }

    window.addEventListener('message', (event) => {
      const message = event.data;
      if (message.type === 'state') {
        renderWelcome(message.welcome);
        renderSuggestion(message.suggestion);
        renderPreview(message.preview);
        renderContexts(message.contexts);
      }
    });
  </script>
</body>
</html>`;
  }
}
