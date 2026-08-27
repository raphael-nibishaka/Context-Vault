import * as fs from "fs/promises";
import * as path from "path";
import * as vscode from "vscode";
import type { ContextSummary, VaultContext } from "./types";
import {
  CONTEXT_INDEX_RELATIVE_PATH,
  CONTEXT_VERSION,
  CONTEXTS_DIR_RELATIVE_PATH,
  LATEST_CONTEXT_RELATIVE_PATH,
} from "./types";
import { toDesktopBridgePayload } from "./contextCapture";

function workspaceRoot(): vscode.Uri | undefined {
  return vscode.workspace.workspaceFolders?.[0]?.uri;
}

function contextsDirUri(): vscode.Uri | undefined {
  const root = workspaceRoot();
  return root ? vscode.Uri.joinPath(root, CONTEXTS_DIR_RELATIVE_PATH) : undefined;
}

function indexUri(): vscode.Uri | undefined {
  const root = workspaceRoot();
  return root ? vscode.Uri.joinPath(root, CONTEXT_INDEX_RELATIVE_PATH) : undefined;
}

function latestUri(): vscode.Uri | undefined {
  const root = workspaceRoot();
  return root ? vscode.Uri.joinPath(root, LATEST_CONTEXT_RELATIVE_PATH) : undefined;
}

function contextFileUri(id: string): vscode.Uri | undefined {
  const dir = contextsDirUri();
  return dir ? vscode.Uri.joinPath(dir, `${id}.json`) : undefined;
}

async function ensureContextsDir(): Promise<vscode.Uri> {
  const dir = contextsDirUri();
  if (!dir) {
    throw new Error("Open a folder or workspace before saving a context.");
  }
  await fs.mkdir(dir.fsPath, { recursive: true });
  return dir;
}

function toSummary(context: VaultContext): ContextSummary {
  return {
    id: context.id,
    name: context.name,
    savedAt: context.savedAt,
    workspace: context.workspace,
    gitBranch: context.gitBranch,
    openFileCount: context.openFiles.length,
    activeFile: context.activeFile,
    nextStep: context.intelligence?.nextStep,
    summary: context.intelligence?.summary,
  };
}

async function readIndex(): Promise<ContextSummary[]> {
  const target = indexUri();
  if (!target) {
    return [];
  }
  try {
    const raw = await fs.readFile(target.fsPath, "utf8");
    const parsed = JSON.parse(raw) as ContextSummary[];
    return Array.isArray(parsed) ? parsed : [];
  } catch (error) {
    const nodeError = error as NodeJS.ErrnoException;
    if (nodeError.code === "ENOENT") {
      return [];
    }
    throw error;
  }
}

async function writeIndex(summaries: ContextSummary[]): Promise<void> {
  await ensureContextsDir();
  const target = indexUri();
  if (!target) {
    throw new Error("Open a folder or workspace before saving a context.");
  }
  await fs.writeFile(target.fsPath, JSON.stringify(summaries, null, 2), "utf8");
}

export async function listContexts(): Promise<ContextSummary[]> {
  const summaries = await readIndex();
  return summaries.sort((a, b) => b.savedAt.localeCompare(a.savedAt));
}

export async function loadContext(id: string): Promise<VaultContext | undefined> {
  const target = contextFileUri(id);
  if (!target) {
    return undefined;
  }
  try {
    const raw = await fs.readFile(target.fsPath, "utf8");
    const parsed = JSON.parse(raw) as VaultContext;
    if ((parsed.version !== 2 && parsed.version !== CONTEXT_VERSION) || !Array.isArray(parsed.tabs)) {
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

export async function saveContext(context: VaultContext): Promise<VaultContext> {
  await ensureContextsDir();
  const file = contextFileUri(context.id);
  const latest = latestUri();
  if (!file || !latest) {
    throw new Error("Open a folder or workspace before saving a context.");
  }

  await fs.writeFile(file.fsPath, JSON.stringify(context, null, 2), "utf8");
  await fs.writeFile(
    latest.fsPath,
    JSON.stringify(toDesktopBridgePayload(context), null, 2),
    "utf8"
  );

  const summaries = (await readIndex()).filter((item) => item.id !== context.id);
  summaries.unshift(toSummary(context));
  await writeIndex(summaries);
  return context;
}

export async function deleteContext(id: string): Promise<boolean> {
  const file = contextFileUri(id);
  if (!file) {
    return false;
  }

  let deleted = false;
  try {
    await fs.unlink(file.fsPath);
    deleted = true;
  } catch (error) {
    const nodeError = error as NodeJS.ErrnoException;
    if (nodeError.code !== "ENOENT") {
      throw error;
    }
  }

  const summaries = (await readIndex()).filter((item) => item.id !== id);
  await writeIndex(summaries);
  return deleted;
}

export function contextsStoragePath(): string | undefined {
  return contextsDirUri()?.fsPath;
}

export function latestContextPath(): string | undefined {
  return latestUri()?.fsPath;
}

export async function exportOpenFilesText(context: VaultContext): Promise<string> {
  return context.openFilePaths.join("\n");
}

export function contextDisplayPath(context: VaultContext): string {
  return path.join(context.workspacePath, CONTEXTS_DIR_RELATIVE_PATH, `${context.id}.json`);
}
