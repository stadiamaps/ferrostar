import { describe, expect, it, vi } from "vitest";
import { NavigationReplay } from "@stadiamaps/ferrostar";
import {
  clearReplayError,
  createReplayFromFile,
  replayErrorMessage,
  showReplayError,
} from "../src/replay-error";
import { replayDelay } from "../src/replay-timing";

describe("replay errors", () => {
  it("throws a native Error when Wasm cannot deserialize a recording", () => {
    expect(() => new NavigationReplay("{}")).toThrowError(
      /failed to deserialize navigation recording: missing field/,
    );

    try {
      new NavigationReplay("{}");
    } catch (error) {
      expect(error).toBeInstanceOf(Error);
    }
  });

  it("preserves Error messages and safely presents them", () => {
    const target = { hidden: true, textContent: "" };
    const error = new Error("missing field `distanceToNextManeuver`");

    expect(replayErrorMessage(error)).toBe(error.message);
    showReplayError(target, error);
    expect(target).toEqual({
      hidden: false,
      textContent:
        "Unable to load recording: missing field `distanceToNextManeuver`",
    });

    clearReplayError(target);
    expect(target).toEqual({ hidden: true, textContent: "" });
  });

  it("does not construct a replay when reading the file fails", async () => {
    const readError = new Error("read failed");
    const createReplay = vi.fn();

    await expect(
      createReplayFromFile(
        { text: () => Promise.reject(readError) },
        createReplay,
      ),
    ).rejects.toBe(readError);
    expect(createReplay).not.toHaveBeenCalled();
  });

  it("propagates replay construction errors", async () => {
    const parseError = new Error("invalid recording");

    await expect(
      createReplayFromFile({ text: () => Promise.resolve("invalid") }, () => {
        throw parseError;
      }),
    ).rejects.toBe(parseError);
  });
});

describe("replay timing", () => {
  it("uses the recording start for the first event delay", () => {
    expect(replayDelay(1003, 1000, 1)).toBe(3);
    expect(replayDelay(1003, 1000, 2)).toBe(1.5);
  });

  it("clamps regressing timestamps to zero", () => {
    expect(replayDelay(999, 1000, 1)).toBe(0);
  });
});
