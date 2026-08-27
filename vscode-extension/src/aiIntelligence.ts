import * as vscode from "vscode";
import type { ContextIntelligence, VaultContext } from "./types";

const SECRET_KEY = "contextVault.openAiApiKey";

function humanizeToken(value: string): string {
  return value
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replace(/[-_]/g, " ")
    .replace(/\.[^.]+$/, "")
    .replace(/\s+/g, " ")
    .trim();
}

function branchTopic(branch: string): string {
  if (!branch) {
    return "your current task";
  }
  const cleaned = branch
    .replace(/^(feature|bugfix|hotfix|release|chore)\//i, "")
    .replace(/[-_]/g, " ")
    .trim();
  return cleaned || branch;
}

function uniqueWorkItems(context: VaultContext): string[] {
  const items = context.openFiles
    .map((file) => humanizeToken(file))
    .filter(Boolean);
  return [...new Set(items)].slice(0, 5);
}

export function buildHeuristicIntelligence(context: VaultContext): ContextIntelligence {
  const topic = branchTopic(context.gitBranch);
  const work = uniqueWorkItems(context);
  const workspace = context.workspace || "this project";
  const summary = `You are implementing ${topic} for the ${workspace} workspace.`;
  const nextStep =
    work.length > 0
      ? `Continue with ${work[0]} and verify related changes on ${context.gitBranch || "the current branch"}.`
      : `Review recent Git changes and decide the next implementation step for ${topic}.`;
  const taskDescription =
    work.length > 0
      ? `${topic}: focus on ${work.slice(0, 3).join(", ")}.`
      : `${topic}: resume work in ${workspace}.`;
  const commitSummary =
    work.length > 0
      ? `${topic}: update ${work.slice(0, 3).join(", ")}`
      : `${topic}: continue implementation`;
  const handoffSummary = [
    summary,
    work.length ? `Current work: ${work.join("; ")}.` : "Current work: resume from the last open files.",
    `Likely next step: ${nextStep}`,
    context.note ? `Note: ${context.note}` : "",
  ]
    .filter(Boolean)
    .join(" ");

  return {
    summary,
    currentWork: work.map((item) => item.charAt(0).toUpperCase() + item.slice(1)),
    nextStep,
    taskDescription,
    commitSummary,
    handoffSummary,
    source: "heuristic",
    generatedAt: new Date().toISOString(),
  };
}

function config(): vscode.WorkspaceConfiguration {
  return vscode.workspace.getConfiguration("contextVault");
}

export async function getStoredApiKey(
  secretStorage: vscode.SecretStorage
): Promise<string | undefined> {
  return secretStorage.get(SECRET_KEY);
}

export async function setStoredApiKey(
  secretStorage: vscode.SecretStorage,
  apiKey: string
): Promise<void> {
  await secretStorage.store(SECRET_KEY, apiKey.trim());
}

export async function clearStoredApiKey(
  secretStorage: vscode.SecretStorage
): Promise<void> {
  await secretStorage.delete(SECRET_KEY);
}

async function callOpenAiCompatible(
  apiKey: string,
  context: VaultContext
): Promise<ContextIntelligence> {
  const endpoint =
    config().get<string>("aiEndpoint") ||
    "https://api.openai.com/v1/chat/completions";
  const model = config().get<string>("aiModel") || "gpt-4o-mini";

  const prompt = {
    workspace: context.workspace,
    branch: context.gitBranch,
    activeFile: context.activeFile,
    openFiles: context.openFiles,
    note: context.note || "",
    selectedText: context.selectedText?.slice(0, 500) || "",
  };

  const response = await fetch(endpoint, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${apiKey}`,
    },
    body: JSON.stringify({
      model,
      temperature: 0.3,
      response_format: { type: "json_object" },
      messages: [
        {
          role: "system",
          content:
            "You are Context Vault. Given a developer workspace snapshot, return JSON with keys: summary, currentWork (string array), nextStep, taskDescription, commitSummary, handoffSummary. Keep it concise and practical. summary should sound like: 'You are implementing X for Y'.",
        },
        {
          role: "user",
          content: JSON.stringify(prompt),
        },
      ],
    }),
  });

  if (!response.ok) {
    throw new Error(`AI request failed (${response.status})`);
  }

  const payload = (await response.json()) as {
    choices?: Array<{ message?: { content?: string } }>;
  };
  const content = payload.choices?.[0]?.message?.content;
  if (!content) {
    throw new Error("AI response was empty");
  }

  const parsed = JSON.parse(content) as Partial<ContextIntelligence>;
  const fallback = buildHeuristicIntelligence(context);
  return {
    summary: parsed.summary?.trim() || fallback.summary,
    currentWork:
      Array.isArray(parsed.currentWork) && parsed.currentWork.length > 0
        ? parsed.currentWork.map(String)
        : fallback.currentWork,
    nextStep: parsed.nextStep?.trim() || fallback.nextStep,
    taskDescription: parsed.taskDescription?.trim() || fallback.taskDescription,
    commitSummary: parsed.commitSummary?.trim() || fallback.commitSummary,
    handoffSummary: parsed.handoffSummary?.trim() || fallback.handoffSummary,
    source: "ai",
    generatedAt: new Date().toISOString(),
  };
}

export async function generateContextIntelligence(
  context: VaultContext,
  secretStorage: vscode.SecretStorage
): Promise<ContextIntelligence> {
  const enabled = config().get<boolean>("enableAi", false);
  if (!enabled) {
    return buildHeuristicIntelligence(context);
  }

  const apiKey = await getStoredApiKey(secretStorage);
  if (!apiKey) {
    return buildHeuristicIntelligence(context);
  }

  try {
    return await callOpenAiCompatible(apiKey, context);
  } catch {
    return buildHeuristicIntelligence(context);
  }
}

export function formatRelativeTime(isoDate: string): string {
  const savedAt = new Date(isoDate).getTime();
  if (Number.isNaN(savedAt)) {
    return "recently";
  }
  const diffMs = Date.now() - savedAt;
  const minutes = Math.floor(diffMs / 60000);
  if (minutes < 1) {
    return "just now";
  }
  if (minutes < 60) {
    return `${minutes} minute${minutes === 1 ? "" : "s"} ago`;
  }
  const hours = Math.floor(minutes / 60);
  if (hours < 48) {
    return `${hours} hour${hours === 1 ? "" : "s"} ago`;
  }
  const days = Math.floor(hours / 24);
  return `${days} day${days === 1 ? "" : "s"} ago`;
}
