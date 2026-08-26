import * as vscode from "vscode";
import { execFile } from "child_process";
import { promisify } from "util";
import * as path from "path";
import type {
  EditorGroupSnapshot,
  EditorSessionSnapshot,
  PositionSnapshot,
  RangeSnapshot,
  TabSnapshot,
  TerminalSnapshot,
  VaultContext,
} from "./types";
import { CONTEXT_VERSION } from "./types";

const execFileAsync = promisify(execFile);

function toPosition(position: vscode.Position): PositionSnapshot {
  return {
    line: position.line,
    character: position.character,
  };
}

function toRange(range: vscode.Range): RangeSnapshot {
  return {
    start: toPosition(range.start),
    end: toPosition(range.end),
  };
}

function resolveUri(input: unknown): vscode.Uri | undefined {
  if (!input || typeof input !== "object") {
    return undefined;
  }
  const candidate = input as { uri?: vscode.Uri };
  return candidate.uri instanceof vscode.Uri ? candidate.uri : undefined;
}

function editorForUri(uri: vscode.Uri): vscode.TextEditor | undefined {
  return vscode.window.visibleTextEditors.find(
    (editor) => editor.document.uri.toString() === uri.toString()
  );
}

function toRelativePath(fsPath: string): string {
  const relative = vscode.workspace.asRelativePath(fsPath, false);
  return relative || fsPath;
}

function fileNameOf(fsPath: string): string {
  const parts = fsPath.replace(/\\/g, "/").split("/");
  return parts[parts.length - 1] || fsPath;
}

function captureTab(tab: vscode.Tab, activeUri: string | undefined): TabSnapshot | undefined {
  const uri = resolveUri(tab.input);
  if (!uri || uri.scheme !== "file") {
    return undefined;
  }

  const editor = editorForUri(uri);
  const cursor = editor ? toPosition(editor.selection.active) : { line: 0, character: 0 };
  const selection = editor
    ? toRange(editor.selection)
    : { start: { line: 0, character: 0 }, end: { line: 0, character: 0 } };
  const selectedText =
    editor && !editor.selection.isEmpty ? editor.document.getText(editor.selection) : undefined;
  const visibleRange =
    editor && editor.visibleRanges.length > 0 ? toRange(editor.visibleRanges[0]) : undefined;

  return {
    fsPath: uri.fsPath,
    relativePath: toRelativePath(uri.fsPath),
    fileName: fileNameOf(uri.fsPath),
    viewColumn: tab.group.viewColumn,
    isActive: activeUri === uri.toString() || tab.isActive,
    isPreview: tab.isPreview,
    cursor,
    selection,
    selectedText,
    visibleRange,
  };
}

function captureTabs(): TabSnapshot[] {
  const activeUri = vscode.window.activeTextEditor?.document.uri.toString();
  const tabs: TabSnapshot[] = [];
  for (const group of vscode.window.tabGroups.all) {
    for (const tab of group.tabs) {
      const snapshot = captureTab(tab, activeUri);
      if (snapshot) {
        tabs.push(snapshot);
      }
    }
  }
  return tabs;
}

function captureEditorGroups(tabs: TabSnapshot[]): EditorGroupSnapshot[] {
  return vscode.window.tabGroups.all.map((group) => {
    const groupTabs = tabs.filter((tab) => tab.viewColumn === group.viewColumn);
    const active = groupTabs.find((tab) => tab.isActive);
    return {
      viewColumn: group.viewColumn,
      isActive: group.isActive,
      tabs: groupTabs.map((tab) => tab.relativePath),
      activeFile: active?.fileName,
    };
  });
}

function captureTerminals(): TerminalSnapshot[] {
  return vscode.window.terminals.map((terminal, index) => ({
    name: terminal.name,
    index,
  }));
}

async function detectGitBranch(workspacePath: string): Promise<string> {
  try {
    const { stdout } = await execFileAsync(
      "git",
      ["-C", workspacePath, "branch", "--show-current"],
      { timeout: 5000, windowsHide: true }
    );
    const branch = stdout.trim();
    return branch && branch !== "HEAD" ? branch : "";
  } catch {
    try {
      const { stdout } = await execFileAsync(
        "git",
        ["-C", workspacePath, "rev-parse", "--abbrev-ref", "HEAD"],
        { timeout: 5000, windowsHide: true }
      );
      const branch = stdout.trim();
      return branch && branch !== "HEAD" ? branch : "";
    } catch {
      return "";
    }
  }
}

function createId(): string {
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}

export function captureEditorSession(): EditorSessionSnapshot | undefined {
  const workspaceFolders =
    vscode.workspace.workspaceFolders?.map((folder) => folder.uri.fsPath) ?? [];
  if (workspaceFolders.length === 0) {
    return undefined;
  }

  const tabs = captureTabs();
  return {
    version: 1,
    savedAt: new Date().toISOString(),
    workspaceName: vscode.workspace.name ?? "workspace",
    workspaceFolders,
    activeFsPath: vscode.window.activeTextEditor?.document.uri.fsPath,
    tabs,
  };
}

export async function captureVaultContext(
  name?: string,
  suggestedPaths?: string[]
): Promise<VaultContext | undefined> {
  const folders = vscode.workspace.workspaceFolders;
  if (!folders || folders.length === 0) {
    return undefined;
  }

  const workspacePath = folders[0].uri.fsPath;
  const workspace = vscode.workspace.name ?? folders[0].name;
  const tabs = captureTabs();
  const activeEditor = vscode.window.activeTextEditor;
  const activeTab = tabs.find((tab) => tab.isActive) ?? tabs[0];
  const gitBranch = await detectGitBranch(workspacePath);
  const selectedText =
    activeEditor && !activeEditor.selection.isEmpty
      ? activeEditor.document.getText(activeEditor.selection)
      : "";

  const contextName =
    name?.trim() ||
    `${workspace}${gitBranch ? ` · ${gitBranch}` : ""} · ${new Date().toLocaleString()}`;

  const openFilePaths =
    suggestedPaths && suggestedPaths.length > 0
      ? suggestedPaths.map((filePath) =>
          path.isAbsolute(filePath) ? toRelativePath(filePath) : filePath.replace(/\\/g, "/")
        )
      : tabs.map((tab) => tab.relativePath);

  const openFiles = openFilePaths.map((relative) => fileNameOf(relative));

  return {
    version: CONTEXT_VERSION,
    id: createId(),
    name: contextName,
    savedAt: new Date().toISOString(),
    workspace,
    workspacePath,
    workspaceFolders: folders.map((folder) => folder.uri.fsPath),
    activeFile: activeTab?.fileName ?? openFiles[0] ?? "",
    activeFilePath: activeTab?.relativePath ?? openFilePaths[0] ?? "",
    openFiles,
    openFilePaths,
    gitBranch,
    cursor: activeEditor ? toPosition(activeEditor.selection.active) : undefined,
    selectedText,
    editorGroups: captureEditorGroups(tabs),
    terminals: captureTerminals(),
    tabs,
  };
}

export function toDesktopBridgePayload(context: VaultContext): Record<string, unknown> {
  return {
    workspace: context.workspace,
    workspacePath: context.workspacePath,
    activeFile: context.activeFile,
    activeFilePath: context.activeFilePath,
    openFiles: context.openFiles,
    openFilePaths: context.openFilePaths,
    gitBranch: context.gitBranch,
    cursor: context.cursor,
    selectedText: context.selectedText,
    editorGroups: context.editorGroups,
    terminals: context.terminals,
    savedAt: context.savedAt,
    name: context.name,
  };
}
