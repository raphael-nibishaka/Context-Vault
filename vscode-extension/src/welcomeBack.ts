import type { VaultContext, WelcomeBackMessage } from "./types";
import { formatRelativeTime } from "./aiIntelligence";

export function buildWelcomeBackMessage(
  context: VaultContext | undefined
): WelcomeBackMessage | undefined {
  if (!context) {
    return undefined;
  }

  const intelligence = context.intelligence;
  return {
    title: "Welcome back",
    contextName: context.name || context.workspace || "your previous context",
    lastActivity: formatRelativeTime(context.savedAt),
    modifiedFileCount: context.openFiles?.length ?? 0,
    lastNote: context.note?.trim() || intelligence?.taskDescription || "No note saved.",
    nextStep:
      intelligence?.nextStep ||
      "Review your open files and continue from the last active editor.",
    summary: intelligence?.summary || `You were working on ${context.name || context.workspace}.`,
    source: intelligence?.source || "none",
  };
}
