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
  viewColumn: number;
  isActive: boolean;
  isPreview?: boolean;
  cursor: PositionSnapshot;
  selection: RangeSnapshot;
  visibleRange?: RangeSnapshot;
}

export interface EditorSessionSnapshot {
  version: 1;
  savedAt: string;
  workspaceName: string;
  workspaceFolders: string[];
  activeFsPath?: string;
  tabs: TabSnapshot[];
}

export const SESSION_FILE_RELATIVE_PATH = ".context-vault/editor-session.json";
export const SESSION_VERSION = 1 as const;
