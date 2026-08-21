import * as vscode from "vscode";
import type {
  EditorSessionSnapshot,
  PositionSnapshot,
  RangeSnapshot,
  TabSnapshot,
} from "./types";

function toPosition(snapshot: PositionSnapshot): vscode.Position {
  return new vscode.Position(
    Math.max(0, snapshot.line),
    Math.max(0, snapshot.character)
  );
}

function toSelection(snapshot: RangeSnapshot): vscode.Selection {
  return new vscode.Selection(toPosition(snapshot.start), toPosition(snapshot.end));
}

function toRange(snapshot: RangeSnapshot): vscode.Range {
  return new vscode.Range(toPosition(snapshot.start), toPosition(snapshot.end));
}

function groupTabsByColumn(tabs: TabSnapshot[]): Map<number, TabSnapshot[]> {
  const groups = new Map<number, TabSnapshot[]>();
  for (const tab of tabs) {
    const column = tab.viewColumn || vscode.ViewColumn.One;
    const existing = groups.get(column) ?? [];
    existing.push(tab);
    groups.set(column, existing);
  }
  return groups;
}

async function openTab(tab: TabSnapshot, preview: boolean): Promise<vscode.TextEditor | undefined> {
  try {
    const document = await vscode.workspace.openTextDocument(vscode.Uri.file(tab.fsPath));
    const editor = await vscode.window.showTextDocument(document, {
      viewColumn: tab.viewColumn || vscode.ViewColumn.One,
      preview,
      preserveFocus: true,
      selection: toSelection(tab.selection),
    });

    editor.selection = toSelection(tab.selection);
    if (tab.visibleRange) {
      editor.revealRange(toRange(tab.visibleRange), vscode.TextEditorRevealType.InCenterIfOutsideViewport);
    } else {
      editor.revealRange(
        new vscode.Range(toPosition(tab.cursor), toPosition(tab.cursor)),
        vscode.TextEditorRevealType.InCenterIfOutsideViewport
      );
    }
    return editor;
  } catch {
    return undefined;
  }
}

export async function restoreEditorSession(
  snapshot: EditorSessionSnapshot
): Promise<{ restored: number; skipped: number }> {
  if (!snapshot.tabs.length) {
    return { restored: 0, skipped: 0 };
  }

  let restored = 0;
  let skipped = 0;
  const groups = groupTabsByColumn(snapshot.tabs);
  let lastActiveEditor: vscode.TextEditor | undefined;

  for (const [, tabs] of [...groups.entries()].sort((a, b) => a[0] - b[0])) {
    for (let index = 0; index < tabs.length; index += 1) {
      const tab = tabs[index];
      const isLastInGroup = index === tabs.length - 1;
      const editor = await openTab(tab, !isLastInGroup && Boolean(tab.isPreview));
      if (!editor) {
        skipped += 1;
        continue;
      }
      restored += 1;
      if (tab.isActive || tab.fsPath === snapshot.activeFsPath) {
        lastActiveEditor = editor;
      }
    }
  }

  if (lastActiveEditor) {
    await vscode.window.showTextDocument(lastActiveEditor.document, {
      viewColumn: lastActiveEditor.viewColumn,
      preview: false,
      preserveFocus: false,
      selection: lastActiveEditor.selection,
    });
  }

  return { restored, skipped };
}
