export type SpeechEngine = {
  speak: (text: string, isMuted: boolean) => void;
  stop: () => void;
};

export const ManualSpeechEngine: SpeechEngine = {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  speak: (_: string, __: boolean) => {
    return;
  },
  stop: () => {
    return;
  },
};
