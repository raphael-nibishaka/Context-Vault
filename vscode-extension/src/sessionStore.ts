import * as fs from "fs/promises";
import * as path from "path";
import * as vscode from "vscode";
import type { EditorSessionSnapshot } from "./types";
import { SESSION_FILE_RELATIVE_PATH, SESSION_VERSION } from "./types";

function sessionFileUri(): vscode.Uri | undefined {
  const folder = vscode.workspace.workspaceFolders?.[0];
  if (!folder) {
    return undefined;
  }
  return vscode.Uri.joinPath(folder.uri, SESSION_FILE_RELATIVE_PATH);
}

export async function saveSession(
  snapshot: EditorSessionSnapshot
): Promise<vscode.Uri> {
  const target = sessionFileUri();
  if (!target) {
    throw new Error("Open a folder or workspace before saving an editor session.");
  }

  const directory = path.dirname(target.fsPath);
  await fs.mkdir(directory, { recursive: true });
  await fs.writeFile(target.fsPath, JSON.stringify(snapshot, null, 2), "utf8");
  return target;
}

export async function loadSession(): Promise<EditorSessionSnapshot | undefined> {
  const target = sessionFileUri();
  if (!target) {
    return undefined;
  }

  try {
    const raw = await fs.readFile(target.fsPath, "utf8");
    const parsed = JSON.parse(raw) as EditorSessionSnapshot;
    if (parsed.version !== SESSION_VERSION || !Array.isArray(parsed.tabs)) {
      return undefined;
    }
    return parsed;
  } catch (error) {
    const nodeError = error as NodeJS.ErrnoException;
    if (nodeError.code === "ENOENT") {
      return undefined;
    }
    throw error;
  }
}

export async function clearSession(): Promise<boolean> {
  const target = sessionFileUri();
  if (!target) {
    return false;
  }

  try {
    await fs.unlink(target.fsPath);
    return true;
  } catch (error) {
    const nodeError = error as NodeJS.ErrnoException;
    if (nodeError.code === "ENOENT") {
      return false;
    }
    throw error;
  }
}

export function sessionStoragePath(): string | undefined {
  return sessionFileUri()?.fsPath;
}
