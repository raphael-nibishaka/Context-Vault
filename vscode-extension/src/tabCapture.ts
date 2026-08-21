import * as vscode from "vscode";
import type {
  EditorSessionSnapshot,
  PositionSnapshot,
  RangeSnapshot,
  TabSnapshot,
} from "./types";

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
  if (candidate.uri instanceof vscode.Uri) {
    return candidate.uri;
  }

  return undefined;
}

function editorForUri(uri: vscode.Uri): vscode.TextEditor | undefined {
  return vscode.window.visibleTextEditors.find(
    (editor) => editor.document.uri.toString() === uri.toString()
  );
}

function captureTab(
  tab: vscode.Tab,
  activeUri: string | undefined
): TabSnapshot | undefined {
  const uri = resolveUri(tab.input);
  if (!uri || uri.scheme !== "file") {
    return undefined;
  }

  const editor = editorForUri(uri);
  const cursor = editor
    ? toPosition(editor.selection.active)
    : { line: 0, character: 0 };
  const selection = editor
    ? toRange(editor.selection)
    : {
        start: { line: 0, character: 0 },
        end: { line: 0, character: 0 },
      };
  const visibleRange =
    editor && editor.visibleRanges.length > 0
      ? toRange(editor.visibleRanges[0])
      : undefined;

  return {
    fsPath: uri.fsPath,
    viewColumn: tab.group.viewColumn,
    isActive: activeUri === uri.toString() || tab.isActive,
    isPreview: tab.isPreview,
    cursor,
    selection,
    visibleRange,
  };
}

export function captureEditorSession(): EditorSessionSnapshot | undefined {
  const workspaceFolders =
    vscode.workspace.workspaceFolders?.map((folder) => folder.uri.fsPath) ?? [];
  if (workspaceFolders.length === 0) {
    return undefined;
  }

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

  const activeFsPath = vscode.window.activeTextEditor?.document.uri.fsPath;

  return {
    version: 1,
    savedAt: new Date().toISOString(),
    workspaceName: vscode.workspace.name ?? "workspace",
    workspaceFolders,
    activeFsPath,
    tabs,
  };
}
