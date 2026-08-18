interface ReplayFile {
  text(): Promise<string>;
}

interface ReplayErrorTarget {
  hidden: boolean;
  textContent: string | null;
}

export const replayErrorMessage = (error: unknown): string => {
  if (error instanceof Error) return error.message;
  if (typeof error === "string") return error;
  return "Unknown error";
};

export const showReplayError = (
  target: ReplayErrorTarget,
  error: unknown,
): void => {
  target.textContent = `Unable to load recording: ${replayErrorMessage(error)}`;
  target.hidden = false;
};

export const clearReplayError = (target: ReplayErrorTarget): void => {
  target.textContent = "";
  target.hidden = true;
};

export const createReplayFromFile = async <T>(
  file: ReplayFile,
  createReplay: (json: string) => T,
): Promise<T> => createReplay(await file.text());
