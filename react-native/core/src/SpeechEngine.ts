export type SpeechEngine = {
  speakTrigger: (text: string) => void;
  stop: () => void;
};

export const ManualSpeechEngine: SpeechEngine = {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  speakTrigger: (_: string) => {
    return;
  },
  stop: () => {
    return;
  },
};
