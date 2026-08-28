import * as fs from "fs/promises";
import * as path from "path";
import * as vscode from "vscode";

export interface DebugEntryRecord {
  id: string;
  errorMessage: string;
  stackTrace?: string;
  errorType: string;
  projectName?: string;
  projectPath?: string;
  sourceFile?: string;
  solution?: string;
  fixCommand?: string;
  relatedContext?: string;
  tags?: string;
  createdAt: string;
  updatedAt: string;
}

export interface SimilarDebugMatch {
  entry: DebugEntryRecord;
  score: number;
}

const ERROR_TYPE_PATTERN =
  /\b([A-Z][A-Za-z0-9]*(?:Error|Exception|Failure|Fault|Timeout|Refused|Denied))\b/;

function vaultRoot(): vscode.Uri | undefined {
  const folder = vscode.workspace.workspaceFolders?.[0];
  return folder ? vscode.Uri.joinPath(folder.uri, ".context-vault") : undefined;
}

function debugEntriesPath(): vscode.Uri | undefined {
  const root = vaultRoot();
  return root ? vscode.Uri.joinPath(root, "debug-entries.json") : undefined;
}

export function extractErrorType(errorMessage: string, stackTrace = ""): string {
  const combined = `${errorMessage}\n${stackTrace}`.trim();
  if (!combined) {
    return "";
  }

  const match = combined.match(ERROR_TYPE_PATTERN);
  if (match?.[1]) {
    return match[1];
  }

  const firstLine = combined.split(/\r?\n/, 1)[0]?.trim() ?? combined;
  return firstLine.length > 80 ? `${firstLine.slice(0, 77)}...` : firstLine;
}

function tokenize(text: string): Set<string> {
  const tokens = new Set<string>();
  for (const token of text.toLowerCase().split(/[^a-z0-9]+/)) {
    if (token.length >= 3) {
      tokens.add(token);
    }
  }
  return tokens;
}

function scoreSimilarity(
  entry: DebugEntryRecord,
  inputErrorType: string,
  inputTokens: Set<string>,
  normalizedInput: string
): number {
  let score = 0;

  if (inputErrorType && inputErrorType.toLowerCase() === entry.errorType.toLowerCase()) {
    score += 60;
  }

  const entryText = [
    entry.errorMessage,
    entry.stackTrace ?? "",
    entry.errorType,
    entry.tags ?? "",
  ]
    .join("\n")
    .toLowerCase();

  const inputLower = normalizedInput.toLowerCase();
  if (inputLower && entryText.includes(inputLower)) {
    score += 35;
  } else if (inputErrorType && entryText.includes(inputErrorType.toLowerCase())) {
    score += 25;
  }

  const entryTokens = tokenize(entryText);
  let overlap = 0;
  for (const token of inputTokens) {
    if (entryTokens.has(token)) {
      overlap += 1;
    }
  }
  score += Math.min(overlap * 8, 32);

  return score;
}

export async function listDebugEntries(): Promise<DebugEntryRecord[]> {
  const target = debugEntriesPath();
  if (!target) {
    return [];
  }

  try {
    const raw = await fs.readFile(target.fsPath, "utf8");
    const parsed = JSON.parse(raw) as DebugEntryRecord[];
    return parsed.sort((left, right) => right.updatedAt.localeCompare(left.updatedAt));
  } catch {
    return [];
  }
}

export async function saveDebugEntry(
  partial: Omit<DebugEntryRecord, "id" | "createdAt" | "updatedAt" | "errorType"> & {
    errorType?: string;
  }
): Promise<DebugEntryRecord> {
  const root = vaultRoot();
  const target = debugEntriesPath();
  if (!root || !target) {
    throw new Error("Open a workspace before saving debug fixes.");
  }

  await fs.mkdir(root.fsPath, { recursive: true });
  const entries = await listDebugEntries();
  const now = new Date().toISOString();
  const entry: DebugEntryRecord = {
    id: `debug-${Date.now()}`,
    errorMessage: partial.errorMessage.trim(),
    stackTrace: partial.stackTrace?.trim() || "",
    errorType:
      partial.errorType?.trim() ||
      extractErrorType(partial.errorMessage, partial.stackTrace ?? ""),
    projectName: partial.projectName?.trim() || "",
    projectPath: partial.projectPath?.trim() || "",
    sourceFile: partial.sourceFile?.trim() || "",
    solution: partial.solution?.trim() || "",
    fixCommand: partial.fixCommand?.trim() || "",
    relatedContext: partial.relatedContext?.trim() || "",
    tags: partial.tags?.trim() || "",
    createdAt: now,
    updatedAt: now,
  };

  entries.unshift(entry);
  await fs.writeFile(target.fsPath, JSON.stringify(entries, null, 2), "utf8");
  return entry;
}

export async function searchDebugEntries(query: string): Promise<DebugEntryRecord[]> {
  const entries = await listDebugEntries();
  const trimmed = query.trim().toLowerCase();
  if (!trimmed) {
    return entries;
  }

  return entries.filter((entry) => {
    const haystack = [
      entry.errorMessage,
      entry.stackTrace,
      entry.errorType,
      entry.projectName,
      entry.sourceFile,
      entry.solution,
      entry.fixCommand,
      entry.relatedContext,
      entry.tags,
    ]
      .join("\n")
      .toLowerCase();
    return haystack.includes(trimmed);
  });
}

export async function findBestSimilarMatch(rawErrorText: string): Promise<SimilarDebugMatch | undefined> {
  const normalizedInput = rawErrorText.trim();
  if (!normalizedInput) {
    return undefined;
  }

  const inputErrorType = extractErrorType(normalizedInput, normalizedInput);
  const inputTokens = tokenize(normalizedInput);
  const entries = await listDebugEntries();

  let best: SimilarDebugMatch | undefined;
  for (const entry of entries) {
    const score = scoreSimilarity(entry, inputErrorType, inputTokens, normalizedInput);
    if (score >= 45 && (!best || score > best.score)) {
      best = { entry, score };
    }
  }

  return best;
}

export function debugEntriesBridgePath(): vscode.Uri | undefined {
  return debugEntriesPath();
}

export async function importDebugEntriesFromBridge(sourcePath: string): Promise<number> {
  const target = debugEntriesPath();
  if (!target) {
    return 0;
  }

  const raw = await fs.readFile(sourcePath, "utf8");
  const imported = JSON.parse(raw) as DebugEntryRecord[];
  const root = vaultRoot();
  if (!root) {
    return 0;
  }

  await fs.mkdir(root.fsPath, { recursive: true });
  await fs.writeFile(target.fsPath, JSON.stringify(imported, null, 2), "utf8");
  return imported.length;
}

export function activeWorkspaceContext(): {
  projectName: string;
  projectPath: string;
  sourceFile: string;
} {
  const folder = vscode.workspace.workspaceFolders?.[0];
  const activeEditor = vscode.window.activeTextEditor;
  return {
    projectName: folder?.name ?? "",
    projectPath: folder?.uri.fsPath ?? "",
    sourceFile: activeEditor
      ? path.basename(activeEditor.document.uri.fsPath)
      : "",
  };
}
