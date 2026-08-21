import * as vscode from "vscode";
import { captureEditorSession } from "./tabCapture";
import { clearSession, loadSession, saveSession, sessionStoragePath } from "./sessionStore";
import { restoreEditorSession } from "./tabRestore";

const RESTORE_FLAG_KEY = "contextVault.restoredForWorkspace";

let saveTimer: NodeJS.Timeout | undefined;
let restoring = false;
let output: vscode.OutputChannel;

function config(): vscode.WorkspaceConfiguration {
  return vscode.workspace.getConfiguration("contextVault");
}

function workspaceKey(): string {
  return (
    vscode.workspace.workspaceFolders?.map((folder) => folder.uri.fsPath).join("|") ??
    "no-workspace"
  );
}

async function persistSession(showMessage: boolean): Promise<void> {
  if (restoring) {
    return;
  }

  const snapshot = captureEditorSession();
  if (!snapshot) {
    if (showMessage) {
      void vscode.window.showWarningMessage(
        "Context Vault: open a folder or workspace before saving tabs."
      );
    }
    return;
  }

  const target = await saveSession(snapshot);
  output.appendLine(
    `Saved ${snapshot.tabs.length} tab(s) to ${target.fsPath} at ${snapshot.savedAt}`
  );

  if (showMessage) {
    void vscode.window.showInformationMessage(
      `Context Vault saved ${snapshot.tabs.length} open tab(s) and cursor position(s).`
    );
  }
}

function scheduleAutoSave(): void {
  if (!config().get<boolean>("autoSave", true) || restoring) {
    return;
  }

  if (saveTimer) {
    clearTimeout(saveTimer);
  }

  const delay = config().get<number>("saveDelayMs", 750);
  saveTimer = setTimeout(() => {
    void persistSession(false).catch((error: unknown) => {
      output.appendLine(`Auto-save failed: ${String(error)}`);
    });
  }, delay);
}

async function restoreIfNeeded(context: vscode.ExtensionContext, force: boolean): Promise<void> {
  if (!force && !config().get<boolean>("autoRestore", true)) {
    return;
  }

  const key = workspaceKey();
  const alreadyRestored = context.workspaceState.get<string>(RESTORE_FLAG_KEY);
  if (!force && alreadyRestored === key) {
    return;
  }

  const snapshot = await loadSession();
  if (!snapshot || snapshot.tabs.length === 0) {
    if (force) {
      void vscode.window.showInformationMessage("Context Vault: no saved editor session found.");
    }
    return;
  }

  restoring = true;
  try {
    const result = await restoreEditorSession(snapshot);
    await context.workspaceState.update(RESTORE_FLAG_KEY, key);
    output.appendLine(
      `Restored ${result.restored} tab(s); skipped ${result.skipped} missing file(s).`
    );

    if (force || result.restored > 0) {
      void vscode.window.showInformationMessage(
        `Context Vault restored ${result.restored} tab(s)` +
          (result.skipped > 0 ? ` (${result.skipped} missing)` : "") +
          "."
      );
    }
  } finally {
    restoring = false;
    scheduleAutoSave();
  }
}

export function activate(context: vscode.ExtensionContext): void {
  output = vscode.window.createOutputChannel("Context Vault");
  context.subscriptions.push(output);
  output.appendLine("Context Vault extension activated.");
  const storagePath = sessionStoragePath();
  if (storagePath) {
    output.appendLine(`Session file: ${storagePath}`);
  }

  context.subscriptions.push(
    vscode.commands.registerCommand("contextVault.saveEditorSession", async () => {
      await persistSession(true);
    }),
    vscode.commands.registerCommand("contextVault.restoreEditorSession", async () => {
      await restoreIfNeeded(context, true);
    }),
    vscode.commands.registerCommand("contextVault.clearEditorSession", async () => {
      const cleared = await clearSession();
      await context.workspaceState.update(RESTORE_FLAG_KEY, undefined);
      void vscode.window.showInformationMessage(
        cleared
          ? "Context Vault cleared the saved editor session."
          : "Context Vault: no saved editor session to clear."
      );
    })
  );

  context.subscriptions.push(
    vscode.window.onDidChangeActiveTextEditor(() => scheduleAutoSave()),
    vscode.window.onDidChangeTextEditorSelection(() => scheduleAutoSave()),
    vscode.window.onDidChangeTextEditorVisibleRanges(() => scheduleAutoSave()),
    vscode.window.tabGroups.onDidChangeTabs(() => scheduleAutoSave()),
    vscode.workspace.onDidChangeConfiguration((event) => {
      if (event.affectsConfiguration("contextVault")) {
        scheduleAutoSave();
      }
    })
  );

  // Give the workbench a moment to settle after startup before restoring.
  setTimeout(() => {
    void restoreIfNeeded(context, false).catch((error: unknown) => {
      output.appendLine(`Auto-restore failed: ${String(error)}`);
    });
  }, 800);
}

export function deactivate(): void {
  if (saveTimer) {
    clearTimeout(saveTimer);
  }

  if (!config().get<boolean>("autoSave", true) || restoring) {
    return;
  }

  const snapshot = captureEditorSession();
  if (!snapshot) {
    return;
  }

  void saveSession(snapshot).catch((error: unknown) => {
    console.error("Context Vault: failed to save session on deactivate", error);
  });
}
