import * as fs from "fs/promises";
import * as path from "path";
import { execFile } from "child_process";
import { promisify } from "util";
import * as vscode from "vscode";
import { activityTracker } from "./activityTracker";
import type { LikelyContextSuggestion, ScoredFile } from "./types";
import {
  SCORE_ACTIVE_FILE,
  SCORE_GIT_CHANGE,
  SCORE_RECENTLY_MODIFIED,
  SCORE_RECENTLY_OPENED,
  SCORE_RELATED_NAME,
  SCORE_SAME_DIRECTORY,
} from "./types";

const execFileAsync = promisify(execFile);
const RECENT_MODIFIED_MS = 24 * 60 * 60 * 1000;

interface Candidate {
  fsPath: string;
  relativePath: string;
  fileName: string;
  score: number;
  reasons: Set<string>;
}

async function runGit(workspacePath: string, args: string[]): Promise<string> {
  try {
    const { stdout } = await execFileAsync("git", ["-C", workspacePath, ...args], {
      timeout: 8000,
      windowsHide: true,
      maxBuffer: 2 * 1024 * 1024,
    });
    return stdout.trim();
  } catch {
    return "";
  }
}

function toRelative(workspacePath: string, fsPath: string): string {
  const relative = path.relative(workspacePath, fsPath);
  return relative && !relative.startsWith("..") ? relative.replace(/\\/g, "/") : fsPath;
}

function fileNameOf(fsPath: string): string {
  return path.basename(fsPath);
}

function stem(fileName: string): string {
  return fileName.replace(/\.[^.]+$/, "").toLowerCase();
}

function extensionOf(fileName: string): string {
  const index = fileName.lastIndexOf(".");
  return index >= 0 ? fileName.slice(index).toLowerCase() : "";
}

function ensureCandidate(map: Map<string, Candidate>, workspacePath: string, fsPath: string): Candidate {
  const normalized = path.normalize(fsPath);
  let candidate = map.get(normalized);
  if (!candidate) {
    candidate = {
      fsPath: normalized,
      relativePath: toRelative(workspacePath, normalized),
      fileName: fileNameOf(normalized),
      score: 0,
      reasons: new Set<string>(),
    };
    map.set(normalized, candidate);
  }
  return candidate;
}

function addScore(candidate: Candidate, points: number, reason: string): void {
  candidate.score += points;
  candidate.reasons.add(reason);
}

async function collectGitChangedFiles(workspacePath: string): Promise<string[]> {
  const status = await runGit(workspacePath, ["status", "--short"]);
  const files: string[] = [];
  for (const line of status.split(/\r?\n/)) {
    if (line.length < 3) {
      continue;
    }
    const raw = line.slice(3).trim();
    if (!raw) {
      continue;
    }
    const renamed = raw.includes(" -> ") ? raw.split(" -> ").pop()!.trim() : raw;
    files.push(path.resolve(workspacePath, renamed));
  }
  return files;
}

async function collectRecentCommitFiles(workspacePath: string): Promise<string[]> {
  const output = await runGit(workspacePath, [
    "log",
    "-5",
    "--name-only",
    "--pretty=format:",
  ]);
  const files: string[] = [];
  for (const line of output.split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed) {
      continue;
    }
    files.push(path.resolve(workspacePath, trimmed));
  }
  return files;
}

async function collectRecentCommitMessages(workspacePath: string): Promise<string[]> {
  const output = await runGit(workspacePath, ["log", "-5", "--pretty=%s"]);
  return output
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
}

async function collectRecentlyModifiedFiles(
  workspacePath: string,
  seedPaths: string[]
): Promise<string[]> {
  const results: string[] = [];
  const cutoff = Date.now() - RECENT_MODIFIED_MS;
  for (const fsPath of seedPaths) {
    try {
      const stats = await fs.stat(fsPath);
      if (stats.isFile() && stats.mtimeMs >= cutoff) {
        results.push(fsPath);
      }
    } catch {
      // Ignore missing files.
    }
  }

  // Also scan sibling files near the active file for recent mtimes.
  const active = vscode.window.activeTextEditor?.document.uri.fsPath;
  if (active) {
    const directory = path.dirname(active);
    try {
      const entries = await fs.readdir(directory);
      for (const entry of entries) {
        const fullPath = path.join(directory, entry);
        try {
          const stats = await fs.stat(fullPath);
          if (stats.isFile() && stats.mtimeMs >= cutoff) {
            results.push(fullPath);
          }
        } catch {
          // Ignore.
        }
      }
    } catch {
      // Ignore unreadable directories.
    }
  }

  return results;
}

function collectOpenFiles(): string[] {
  const files: string[] = [];
  for (const group of vscode.window.tabGroups.all) {
    for (const tab of group.tabs) {
      const input = tab.input as { uri?: vscode.Uri } | undefined;
      if (input?.uri?.scheme === "file") {
        files.push(input.uri.fsPath);
      }
    }
  }
  const active = vscode.window.activeTextEditor?.document;
  if (active?.uri.scheme === "file") {
    files.push(active.uri.fsPath);
  }
  return files;
}

export async function detectLikelyContext(
  maxFiles = 8
): Promise<LikelyContextSuggestion | undefined> {
  const folder = vscode.workspace.workspaceFolders?.[0];
  if (!folder) {
    return undefined;
  }

  const workspacePath = folder.uri.fsPath;
  const workspace = vscode.workspace.name ?? folder.name;
  const branch = await runGit(workspacePath, ["branch", "--show-current"]);
  const activeFsPath = vscode.window.activeTextEditor?.document.uri.fsPath;
  const activeFile = activeFsPath ? fileNameOf(activeFsPath) : "";
  const activeDir = activeFsPath ? path.dirname(activeFsPath) : "";
  const activeStem = activeFile ? stem(activeFile) : "";
  const activeExt = activeFile ? extensionOf(activeFile) : "";

  const openFiles = collectOpenFiles();
  const recentlyOpened = activityTracker.getRecentlyOpenedPaths();
  const gitChanged = await collectGitChangedFiles(workspacePath);
  const commitFiles = await collectRecentCommitFiles(workspacePath);
  const recentCommits = await collectRecentCommitMessages(workspacePath);
  const seedPaths = [
    ...openFiles,
    ...recentlyOpened,
    ...gitChanged,
    ...commitFiles,
  ];
  const recentlyModified = await collectRecentlyModifiedFiles(workspacePath, seedPaths);

  const candidates = new Map<string, Candidate>();

  for (const fsPath of openFiles) {
    ensureCandidate(candidates, workspacePath, fsPath);
  }
  for (const fsPath of recentlyOpened) {
    ensureCandidate(candidates, workspacePath, fsPath);
  }
  for (const fsPath of gitChanged) {
    ensureCandidate(candidates, workspacePath, fsPath);
  }
  for (const fsPath of commitFiles) {
    ensureCandidate(candidates, workspacePath, fsPath);
  }
  for (const fsPath of recentlyModified) {
    ensureCandidate(candidates, workspacePath, fsPath);
  }

  for (const candidate of candidates.values()) {
    if (activeFsPath && path.normalize(candidate.fsPath) === path.normalize(activeFsPath)) {
      addScore(candidate, SCORE_ACTIVE_FILE, "Active file");
    }
    if (recentlyModified.some((item) => path.normalize(item) === candidate.fsPath)) {
      addScore(candidate, SCORE_RECENTLY_MODIFIED, "Recently modified");
    }
    if (activeDir && path.dirname(candidate.fsPath) === activeDir) {
      addScore(candidate, SCORE_SAME_DIRECTORY, "Same directory");
    }
    if (gitChanged.some((item) => path.normalize(item) === candidate.fsPath)) {
      addScore(candidate, SCORE_GIT_CHANGE, "Git change");
    }
    if (recentlyOpened.some((item) => path.normalize(item) === candidate.fsPath)) {
      addScore(candidate, SCORE_RECENTLY_OPENED, "Recently opened");
    }
    if (activeStem) {
      const candidateStem = stem(candidate.fileName);
      const related =
        candidateStem.includes(activeStem) ||
        activeStem.includes(candidateStem) ||
        (activeExt && extensionOf(candidate.fileName) === activeExt);
      if (related && (!activeFsPath || candidate.fsPath !== path.normalize(activeFsPath))) {
        addScore(candidate, SCORE_RELATED_NAME, "Related name/extension");
      }
    }
  }

  const ranked: ScoredFile[] = [...candidates.values()]
    .filter((candidate) => candidate.score > 0)
    .sort((a, b) => b.score - a.score || a.relativePath.localeCompare(b.relativePath))
    .slice(0, maxFiles)
    .map((candidate) => ({
      fsPath: candidate.fsPath,
      relativePath: candidate.relativePath,
      fileName: candidate.fileName,
      score: candidate.score,
      reasons: [...candidate.reasons],
    }));

  if (ranked.length === 0) {
    return {
      branch,
      workspace,
      activeFile,
      recentCommits,
      recentTerminalCommands: activityTracker.getRecentTerminalCommands(),
      files: [],
      prompt: "No likely task files detected yet. Open or edit files to improve suggestions.",
    };
  }

  return {
    branch,
    workspace,
    activeFile,
    recentCommits,
    recentTerminalCommands: activityTracker.getRecentTerminalCommands(),
    files: ranked,
    prompt: `Save these ${ranked.length} files as a context?`,
  };
}
