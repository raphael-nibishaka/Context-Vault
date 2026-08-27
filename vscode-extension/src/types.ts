export interface PositionSnapshot {
  line: number;
  character: number;
}

export interface RangeSnapshot {
  start: PositionSnapshot;
  end: PositionSnapshot;
}

export interface TabSnapshot {
  fsPath: string;
  relativePath: string;
  fileName: string;
  viewColumn: number;
  isActive: boolean;
  isPreview?: boolean;
  cursor: PositionSnapshot;
  selection: RangeSnapshot;
  selectedText?: string;
  visibleRange?: RangeSnapshot;
}

export interface EditorGroupSnapshot {
  viewColumn: number;
  isActive: boolean;
  tabs: string[];
  activeFile?: string;
}

export interface TerminalSnapshot {
  name: string;
  index: number;
}

export interface EditorSessionSnapshot {
  version: 1;
  savedAt: string;
  workspaceName: string;
  workspaceFolders: string[];
  activeFsPath?: string;
  tabs: TabSnapshot[];
}

export interface ContextIntelligence {
  summary: string;
  currentWork: string[];
  nextStep: string;
  taskDescription: string;
  commitSummary: string;
  handoffSummary: string;
  source: "ai" | "heuristic";
  generatedAt: string;
}

export interface VaultContext {
  version: 2 | 3;
  id: string;
  name: string;
  savedAt: string;
  workspace: string;
  workspacePath: string;
  workspaceFolders: string[];
  activeFile: string;
  activeFilePath: string;
  openFiles: string[];
  openFilePaths: string[];
  gitBranch: string;
  cursor?: PositionSnapshot;
  selectedText: string;
  editorGroups: EditorGroupSnapshot[];
  terminals: TerminalSnapshot[];
  tabs: TabSnapshot[];
  note?: string;
  intelligence?: ContextIntelligence;
}

export interface ContextSummary {
  id: string;
  name: string;
  savedAt: string;
  workspace: string;
  gitBranch: string;
  openFileCount: number;
  activeFile: string;
  nextStep?: string;
  summary?: string;
}

export interface ScoredFile {
  fsPath: string;
  relativePath: string;
  fileName: string;
  score: number;
  reasons: string[];
}

export interface LikelyContextSuggestion {
  branch: string;
  workspace: string;
  activeFile: string;
  recentCommits: string[];
  recentTerminalCommands: string[];
  files: ScoredFile[];
  prompt: string;
}

export interface WelcomeBackMessage {
  title: string;
  contextName: string;
  lastActivity: string;
  modifiedFileCount: number;
  lastNote: string;
  nextStep: string;
  summary: string;
  source: "ai" | "heuristic" | "none";
}

export const SESSION_FILE_RELATIVE_PATH = ".context-vault/editor-session.json";
export const CONTEXTS_DIR_RELATIVE_PATH = ".context-vault/contexts";
export const LATEST_CONTEXT_RELATIVE_PATH = ".context-vault/latest-context.json";
export const CONTEXT_INDEX_RELATIVE_PATH = ".context-vault/contexts/index.json";
export const SESSION_VERSION = 1 as const;
export const CONTEXT_VERSION = 3 as const;

export const SCORE_ACTIVE_FILE = 30;
export const SCORE_RECENTLY_MODIFIED = 20;
export const SCORE_SAME_DIRECTORY = 15;
export const SCORE_GIT_CHANGE = 20;
export const SCORE_RECENTLY_OPENED = 10;
export const SCORE_RELATED_NAME = 5;
