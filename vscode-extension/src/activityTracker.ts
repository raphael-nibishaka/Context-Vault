import * as vscode from "vscode";

const MAX_RECENT_FILES = 40;
const MAX_TERMINAL_EVENTS = 30;

export class ActivityTracker {
  private readonly recentFiles = new Map<string, number>();
  private readonly terminalCommands: Array<{ command: string; at: number }> = [];

  trackOpenedFile(fsPath: string | undefined): void {
    if (!fsPath) {
      return;
    }
    this.recentFiles.set(fsPath, Date.now());
    this.trimFiles();
  }

  trackTerminalCommand(command: string | undefined): void {
    const trimmed = command?.trim();
    if (!trimmed) {
      return;
    }
    this.terminalCommands.unshift({ command: trimmed, at: Date.now() });
    if (this.terminalCommands.length > MAX_TERMINAL_EVENTS) {
      this.terminalCommands.length = MAX_TERMINAL_EVENTS;
    }
  }

  trackTerminalOpened(name: string | undefined): void {
    if (!name) {
      return;
    }
    this.trackTerminalCommand(`[terminal] ${name}`);
  }

  getRecentlyOpenedPaths(withinMs = 2 * 60 * 60 * 1000): string[] {
    const cutoff = Date.now() - withinMs;
    return [...this.recentFiles.entries()]
      .filter(([, openedAt]) => openedAt >= cutoff)
      .sort((a, b) => b[1] - a[1])
      .map(([fsPath]) => fsPath);
  }

  getRecentTerminalCommands(limit = 8): string[] {
    return this.terminalCommands.slice(0, limit).map((entry) => entry.command);
  }

  attach(context: vscode.ExtensionContext): void {
    const active = vscode.window.activeTextEditor?.document;
    if (active?.uri.scheme === "file") {
      this.trackOpenedFile(active.uri.fsPath);
    }

    context.subscriptions.push(
      vscode.window.onDidChangeActiveTextEditor((editor) => {
        if (editor?.document.uri.scheme === "file") {
          this.trackOpenedFile(editor.document.uri.fsPath);
        }
      }),
      vscode.workspace.onDidOpenTextDocument((document) => {
        if (document.uri.scheme === "file") {
          this.trackOpenedFile(document.uri.fsPath);
        }
      }),
      vscode.window.onDidOpenTerminal((terminal) => {
        this.trackTerminalOpened(terminal.name);
        this.attachShellListener(terminal);
      })
    );

    for (const terminal of vscode.window.terminals) {
      this.attachShellListener(terminal);
    }
  }

  private attachShellListener(terminal: vscode.Terminal): void {
    // Shell integration is available on newer VS Code builds.
    const anyTerminal = terminal as vscode.Terminal & {
      shellIntegration?: {
        onDidStartTerminalShellExecution?: vscode.Event<{
          execution: { commandLine?: { value?: string } | string };
        }>;
      };
    };

    const event = anyTerminal.shellIntegration?.onDidStartTerminalShellExecution;
    if (!event) {
      return;
    }

    event((eventData) => {
      const commandLine = eventData.execution.commandLine;
      const value =
        typeof commandLine === "string" ? commandLine : commandLine?.value;
      this.trackTerminalCommand(value);
    });
  }

  private trimFiles(): void {
    if (this.recentFiles.size <= MAX_RECENT_FILES) {
      return;
    }
    const sorted = [...this.recentFiles.entries()].sort((a, b) => b[1] - a[1]);
    this.recentFiles.clear();
    for (const [fsPath, openedAt] of sorted.slice(0, MAX_RECENT_FILES)) {
      this.recentFiles.set(fsPath, openedAt);
    }
  }
}

export const activityTracker = new ActivityTracker();
