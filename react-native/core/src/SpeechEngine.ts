export type SpeechEngine = {
  speak: (text: string) => void;
  stop: () => void;
};

export const ManualSpeechEngine: SpeechEngine = {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  speak: (_: string) => {
    return;
  },
  stop: () => {
    return;
  },
};
