import * as vscode from "vscode";
import { activityTracker } from "./activityTracker";
import { captureEditorSession, captureVaultContext } from "./contextCapture";
import {
  deleteContext,
  latestContextPath,
  listContexts,
  loadContext,
  saveContext,
} from "./contextStore";
import { clearSession, loadSession, saveSession, sessionStoragePath } from "./sessionStore";
import { ContextVaultViewProvider } from "./sidebarProvider";
import { detectLikelyContext } from "./smartDetection";
import { restoreEditorSession, restoreVaultContext } from "./tabRestore";
import type { LikelyContextSuggestion } from "./types";

const RESTORE_FLAG_KEY = "contextVault.restoredForWorkspace";

let saveTimer: NodeJS.Timeout | undefined;
let restoring = false;
let output: vscode.OutputChannel;
let sidebar: ContextVaultViewProvider;
let latestSuggestion: LikelyContextSuggestion | undefined;

function config(): vscode.WorkspaceConfiguration {
  return vscode.workspace.getConfiguration("contextVault");
}

function workspaceKey(): string {
  return (
    vscode.workspace.workspaceFolders?.map((folder) => folder.uri.fsPath).join("|") ??
    "no-workspace"
  );
}

async function refreshSidebarPreview(): Promise<void> {
  const preview = await captureVaultContext("preview");
  latestSuggestion = await detectLikelyContext(config().get<number>("suggestionLimit", 8));
  sidebar.setPreview(preview);
  sidebar.setSuggestion(latestSuggestion);
  sidebar.setContexts(await listContexts());
}

async function persistAutoSession(showMessage: boolean): Promise<void> {
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
    `Auto-saved ${snapshot.tabs.length} tab(s) to ${target.fsPath} at ${snapshot.savedAt}`
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
    void persistAutoSession(false).catch((error: unknown) => {
      output.appendLine(`Auto-save failed: ${String(error)}`);
    });
    void refreshSidebarPreview().catch(() => undefined);
  }, delay);
}

async function saveNamedContext(suggestedPaths?: string[]): Promise<void> {
  const draft = await captureVaultContext(undefined, suggestedPaths);
  if (!draft) {
    void vscode.window.showWarningMessage(
      "Context Vault: open a folder or workspace before saving a context."
    );
    return;
  }

  const defaultName = suggestedPaths?.length
    ? `${draft.workspace}${draft.gitBranch ? ` · ${draft.gitBranch}` : ""} · smart`
    : `${draft.workspace}${draft.gitBranch ? ` · ${draft.gitBranch}` : ""}`;

  const name = await vscode.window.showInputBox({
    title: "Save Context",
    prompt: suggestedPaths?.length
      ? `Save these ${suggestedPaths.length} suggested files as a context`
      : "Name this development context",
    value: defaultName,
    ignoreFocusOut: true,
  });

  if (name === undefined) {
    return;
  }

  draft.name = name.trim() || draft.name;
  const saved = await saveContext(draft);
  await persistAutoSession(false);
  await refreshSidebarPreview();

  output.appendLine(
    `Saved context "${saved.name}" with ${saved.openFiles.length} file(s), branch ${saved.gitBranch || "n/a"}`
  );
  const bridge = latestContextPath();
  if (bridge) {
    output.appendLine(`Desktop bridge file: ${bridge}`);
  }

  void vscode.window.showInformationMessage(
    `Context Vault saved "${saved.name}" · ${saved.openFiles.length} open file(s)` +
      (saved.gitBranch ? ` · ${saved.gitBranch}` : "")
  );
}

async function saveSuggestedContext(): Promise<void> {
  if (!latestSuggestion || latestSuggestion.files.length === 0) {
    latestSuggestion = await detectLikelyContext(config().get<number>("suggestionLimit", 8));
  }
  if (!latestSuggestion || latestSuggestion.files.length === 0) {
    void vscode.window.showInformationMessage(
      "Context Vault: no likely task files detected yet."
    );
    return;
  }

  await saveNamedContext(latestSuggestion.files.map((file) => file.relativePath));
}

async function restoreNamedContext(id?: string): Promise<void> {
  const contexts = await listContexts();
  if (contexts.length === 0) {
    void vscode.window.showInformationMessage("Context Vault: no saved contexts yet.");
    return;
  }

  let targetId = id;
  if (!targetId) {
    const picked = await vscode.window.showQuickPick(
      contexts.map((context) => ({
        label: context.name,
        description: `${context.gitBranch || "no branch"} · ${context.openFileCount} files`,
        detail: context.activeFile || context.workspace,
        id: context.id,
      })),
      { title: "Restore Context", placeHolder: "Choose a saved context" }
    );
    if (!picked) {
      return;
    }
    targetId = picked.id;
  }

  const context = await loadContext(targetId);
  if (!context) {
    void vscode.window.showWarningMessage("Context Vault: that context could not be loaded.");
    await refreshSidebarPreview();
    return;
  }

  restoring = true;
  try {
    const result = await restoreVaultContext(context);
    await vscode.commands.executeCommand("workbench.action.focusActiveEditorGroup");
    output.appendLine(
      `Restored context "${context.name}": ${result.restored} file(s), ${result.terminalsCreated} terminal(s); skipped ${result.skipped}`
    );
    void vscode.window.showInformationMessage(
      `Restored "${context.name}" · ${result.restored} file(s)` +
        (result.terminalsCreated > 0 ? ` · ${result.terminalsCreated} terminal(s)` : "")
    );
  } finally {
    restoring = false;
    scheduleAutoSave();
  }
}

async function restoreLatestContext(): Promise<void> {
  const contexts = await listContexts();
  if (contexts.length === 0) {
    void vscode.window.showInformationMessage("Context Vault: no saved contexts yet.");
    return;
  }
  await restoreNamedContext(contexts[0].id);
}

async function deleteNamedContext(id: string): Promise<void> {
  const context = await loadContext(id);
  const label = context?.name ?? "this context";
  const confirm = await vscode.window.showWarningMessage(
    `Delete "${label}"?`,
    { modal: true },
    "Delete"
  );
  if (confirm !== "Delete") {
    return;
  }
  await deleteContext(id);
  await refreshSidebarPreview();
  void vscode.window.showInformationMessage(`Context Vault deleted "${label}".`);
}

async function restoreAutoSession(
  context: vscode.ExtensionContext,
  force: boolean
): Promise<void> {
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
      `Restored auto-session: ${result.restored} tab(s); skipped ${result.skipped}`
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
  output.appendLine("Context Vault V5 extension activated.");

  activityTracker.attach(context);

  const storagePath = sessionStoragePath();
  if (storagePath) {
    output.appendLine(`Auto-session file: ${storagePath}`);
  }
  const bridge = latestContextPath();
  if (bridge) {
    output.appendLine(`Desktop bridge file: ${bridge}`);
  }

  sidebar = new ContextVaultViewProvider(context.extensionUri, {
    onSave: () => saveNamedContext(),
    onSaveSuggestion: saveSuggestedContext,
    onRestoreLatest: restoreLatestContext,
    onRestore: restoreNamedContext,
    onDelete: deleteNamedContext,
    onRefresh: refreshSidebarPreview,
  });

  context.subscriptions.push(
    vscode.window.registerWebviewViewProvider(ContextVaultViewProvider.viewType, sidebar)
  );

  context.subscriptions.push(
    vscode.commands.registerCommand("contextVault.saveContext", () => saveNamedContext()),
    vscode.commands.registerCommand("contextVault.saveSuggestedContext", () =>
      saveSuggestedContext()
    ),
    vscode.commands.registerCommand("contextVault.detectLikelyContext", async () => {
      await refreshSidebarPreview();
      await vscode.commands.executeCommand("contextVault.sidebar.focus");
      if (latestSuggestion?.files.length) {
        void vscode.window.showInformationMessage(
          `Likely context: ${latestSuggestion.files.map((file) => file.fileName).join(", ")}`
        );
      }
    }),
    vscode.commands.registerCommand("contextVault.restoreContext", () => restoreLatestContext()),
    vscode.commands.registerCommand("contextVault.showContexts", async () => {
      await vscode.commands.executeCommand("contextVault.sidebar.focus");
      await refreshSidebarPreview();
    }),
    vscode.commands.registerCommand("contextVault.saveEditorSession", async () => {
      await persistAutoSession(true);
    }),
    vscode.commands.registerCommand("contextVault.restoreEditorSession", async () => {
      await restoreAutoSession(context, true);
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
    vscode.window.onDidOpenTerminal(() => scheduleAutoSave()),
    vscode.window.onDidCloseTerminal(() => scheduleAutoSave()),
    vscode.workspace.onDidSaveTextDocument(() => scheduleAutoSave()),
    vscode.workspace.onDidChangeConfiguration((event) => {
      if (event.affectsConfiguration("contextVault")) {
        scheduleAutoSave();
      }
    })
  );

  setTimeout(() => {
    void restoreAutoSession(context, false).catch((error: unknown) => {
      output.appendLine(`Auto-restore failed: ${String(error)}`);
    });
    void refreshSidebarPreview().catch((error: unknown) => {
      output.appendLine(`Sidebar refresh failed: ${String(error)}`);
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
