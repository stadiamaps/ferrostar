export const replayDelay = (
  timestamp: number,
  previousTimestamp: number,
  playbackSpeed: number,
): number => Math.max(0, (timestamp - previousTimestamp) / playbackSpeed);
