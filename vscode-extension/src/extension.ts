import * as vscode from "vscode";
import { activityTracker } from "./activityTracker";
import {
  clearStoredApiKey,
  generateContextIntelligence,
  setStoredApiKey,
} from "./aiIntelligence";
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
import { buildWelcomeBackMessage } from "./welcomeBack";
import {
  activeWorkspaceContext,
  debugEntriesBridgePath,
  findBestSimilarMatch,
  saveDebugEntry,
  searchDebugEntries,
} from "./debugMemory";

const RESTORE_FLAG_KEY = "contextVault.restoredForWorkspace";
const WELCOME_SHOWN_KEY = "contextVault.welcomeShownFor";

let saveTimer: NodeJS.Timeout | undefined;
let restoring = false;
let output: vscode.OutputChannel;
let sidebar: ContextVaultViewProvider;
let latestSuggestion: LikelyContextSuggestion | undefined;
let extensionContext: vscode.ExtensionContext;

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
  const contexts = await listContexts();
  const latest = contexts.length > 0 ? await loadContext(contexts[0].id) : undefined;
  if (latest?.intelligence && preview) {
    preview.intelligence = latest.intelligence;
    preview.note = latest.note;
  }
  sidebar.setPreview(preview);
  sidebar.setSuggestion(latestSuggestion);
  sidebar.setWelcome(buildWelcomeBackMessage(latest));
  sidebar.setContexts(contexts);
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

  const optionalNote = await vscode.window.showInputBox({
    title: "Optional Note",
    prompt: "Add a short note for your future self (optional)",
    value: draft.note || "",
    ignoreFocusOut: true,
  });
  if (optionalNote !== undefined) {
    draft.note = optionalNote.trim();
  }

  await vscode.window.withProgress(
    {
      location: vscode.ProgressLocation.Notification,
      title: "Context Vault is generating a context summary…",
      cancellable: false,
    },
    async () => {
      draft.intelligence = await generateContextIntelligence(
        draft,
        extensionContext.secrets
      );
    }
  );

  const saved = await saveContext(draft);
  await persistAutoSession(false);
  await refreshSidebarPreview();

  output.appendLine(
    `Saved context "${saved.name}" with ${saved.openFiles.length} file(s), branch ${saved.gitBranch || "n/a"}`
  );
  output.appendLine(
    `Intelligence (${saved.intelligence?.source}): ${saved.intelligence?.summary || "n/a"}`
  );
  const bridge = latestContextPath();
  if (bridge) {
    output.appendLine(`Desktop bridge file: ${bridge}`);
  }

  void vscode.window.showInformationMessage(
    `Saved "${saved.name}"` +
      (saved.intelligence?.nextStep ? ` · Next: ${saved.intelligence.nextStep}` : ""),
    "Show Summary"
  ).then((choice) => {
    if (choice === "Show Summary" && saved.intelligence) {
      void vscode.window.showInformationMessage(
        [
          saved.intelligence.summary,
          "",
          "Current work:",
          ...saved.intelligence.currentWork.map((item) => `• ${item}`),
          "",
          `Likely next step: ${saved.intelligence.nextStep}`,
        ].join("\n"),
        { modal: true }
      );
    }
  });
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

async function saveDebugFixFromEditor(): Promise<void> {
  const editor = vscode.window.activeTextEditor;
  const selected = editor?.document.getText(editor.selection);
  let clipboard = "";
  try {
    clipboard = await vscode.env.clipboard.readText();
  } catch {
    clipboard = "";
  }
  const errorText = (selected?.trim() || clipboard.trim()).trim();

  if (!errorText) {
    void vscode.window.showWarningMessage(
      "Context Vault: select an error in the editor or copy one to the clipboard first."
    );
    return;
  }

  const workspace = activeWorkspaceContext();
  const similar = await findBestSimilarMatch(errorText);
  if (similar) {
    const choice = await vscode.window.showInformationMessage(
      `You've seen something similar before: ${similar.entry.errorType}`,
      "View Previous Fix",
      "Log New Fix Anyway"
    );
    if (choice === "View Previous Fix") {
      await showDebugFixDetails(similar.entry);
      return;
    }
  }

  const solution = await vscode.window.showInputBox({
    title: "Debug Fix Solution",
    prompt: "What fixed it?",
    ignoreFocusOut: true,
  });
  if (solution === undefined) {
    return;
  }

  const fixCommand = await vscode.window.showInputBox({
    title: "Fix Command",
    prompt: "Optional command to rerun the fix (e.g. docker compose up mongodb)",
    ignoreFocusOut: true,
  });
  if (fixCommand === undefined) {
    return;
  }

  const saved = await saveDebugEntry({
    errorMessage: errorText.split(/\r?\n/, 1)[0] ?? errorText,
    stackTrace: errorText.includes("\n") ? errorText : "",
    projectName: workspace.projectName,
    projectPath: workspace.projectPath,
    sourceFile: workspace.sourceFile,
    solution: solution.trim(),
    fixCommand: fixCommand.trim(),
    relatedContext: "",
    tags: "",
  });

  output.appendLine(`Saved debug fix "${saved.errorType}" to ${debugEntriesBridgePath()?.fsPath ?? "n/a"}`);
  void vscode.window.showInformationMessage(`Saved debug fix for ${saved.errorType}.`);
}

async function showDebugFixDetails(entry: {
  errorType: string;
  errorMessage: string;
  stackTrace?: string;
  solution?: string;
  fixCommand?: string;
  relatedContext?: string;
}): Promise<void> {
  const lines = [
    entry.errorMessage,
    "",
    entry.stackTrace ? `Stack trace\n${entry.stackTrace}\n` : "",
    entry.solution ? `Solution\n${entry.solution}\n` : "",
    entry.fixCommand ? `Fix command\n${entry.fixCommand}\n` : "",
    entry.relatedContext ? `Related context\n${entry.relatedContext}` : "",
  ]
    .filter(Boolean)
    .join("\n");

  await vscode.window.showInformationMessage(entry.errorType, { modal: true, detail: lines });
}

async function checkSimilarError(): Promise<void> {
  const editor = vscode.window.activeTextEditor;
  const selected = editor?.document.getText(editor.selection);
  let clipboard = "";
  try {
    clipboard = await vscode.env.clipboard.readText();
  } catch {
    clipboard = "";
  }
  const errorText = (selected?.trim() || clipboard.trim()).trim();

  if (!errorText) {
    void vscode.window.showWarningMessage(
      "Context Vault: select an error in the editor or copy one to the clipboard first."
    );
    return;
  }

  const match = await findBestSimilarMatch(errorText);
  if (!match) {
    void vscode.window.showInformationMessage("Context Vault: no similar debug fix found yet.");
    return;
  }

  const choice = await vscode.window.showInformationMessage(
    `You've seen something similar before: ${match.entry.errorType}. ${match.entry.solution || match.entry.fixCommand || ""}`,
    "View Previous Fix",
    "Log New Fix"
  );
  if (choice === "View Previous Fix") {
    await showDebugFixDetails(match.entry);
  } else if (choice === "Log New Fix") {
    await saveDebugFixFromEditor();
  }
}

async function searchDebugHistory(): Promise<void> {
  const query = await vscode.window.showInputBox({
    title: "Search Debugging History",
    prompt: 'Try "Mongo connection" or an error type',
    ignoreFocusOut: true,
  });
  if (query === undefined) {
    return;
  }

  const results = await searchDebugEntries(query);
  if (results.length === 0) {
    void vscode.window.showInformationMessage("Context Vault: no debug fixes matched that search.");
    return;
  }

  const picked = await vscode.window.showQuickPick(
    results.map((entry) => ({
      label: entry.errorType || entry.errorMessage,
      description: entry.solution || entry.fixCommand || "No solution recorded",
      detail: entry.sourceFile ? `${entry.projectName || "Project"} · ${entry.sourceFile}` : entry.projectName,
      entry,
    })),
    { title: "Debugging History", placeHolder: "Choose a saved fix" }
  );

  if (picked) {
    await showDebugFixDetails(picked.entry);
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
  extensionContext = context;
  output = vscode.window.createOutputChannel("Context Vault");
  context.subscriptions.push(output);
  output.appendLine("Context Vault V7 extension activated.");

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
    vscode.commands.registerCommand("contextVault.setAiApiKey", async () => {
      const apiKey = await vscode.window.showInputBox({
        title: "Context Vault AI API Key",
        prompt: "Stored securely in VS Code Secret Storage. Leave blank to clear.",
        password: true,
        ignoreFocusOut: true,
      });
      if (apiKey === undefined) {
        return;
      }
      if (!apiKey.trim()) {
        await clearStoredApiKey(context.secrets);
        void vscode.window.showInformationMessage("Context Vault AI API key cleared.");
        return;
      }
      await setStoredApiKey(context.secrets, apiKey);
      await vscode.workspace
        .getConfiguration("contextVault")
        .update("enableAi", true, vscode.ConfigurationTarget.Global);
      void vscode.window.showInformationMessage(
        "Context Vault AI API key saved. AI summaries are now enabled."
      );
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
    }),
    vscode.commands.registerCommand("contextVault.saveDebugFix", () => saveDebugFixFromEditor()),
    vscode.commands.registerCommand("contextVault.checkSimilarError", () => checkSimilarError()),
    vscode.commands.registerCommand("contextVault.searchDebugHistory", () => searchDebugHistory())
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
    void refreshSidebarPreview()
      .then(async () => {
        const contexts = await listContexts();
        const latest = contexts.length > 0 ? await loadContext(contexts[0].id) : undefined;
        const welcome = buildWelcomeBackMessage(latest);
        if (!welcome || !latest) {
          return;
        }
        const shownFor = context.workspaceState.get<string>(WELCOME_SHOWN_KEY);
        if (shownFor === latest.id) {
          return;
        }
        await context.workspaceState.update(WELCOME_SHOWN_KEY, latest.id);
        void vscode.window.showInformationMessage(
          `Welcome back — you were working on ${welcome.contextName} (${welcome.lastActivity}). Next: ${welcome.nextStep}`,
          "Open Context Vault"
        ).then((choice) => {
          if (choice === "Open Context Vault") {
            void vscode.commands.executeCommand("contextVault.sidebar.focus");
          }
        });
      })
      .catch((error: unknown) => {
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
