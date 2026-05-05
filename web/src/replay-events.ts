import { NavigationRecordingEvent } from "@stadiamaps/ferrostar";

export interface ImportantEvent {
  offsetMs: number;
  type: string;
  label: string;
  tagClass: string;
}

const GPS_GAP_THRESHOLD_MS = 10_000;

type Phase = "pending" | "navigating" | "complete" | "ended";

export const extractImportantEvents = (
  events: NavigationRecordingEvent[],
): ImportantEvent[] => {
  if (!events.length) return [];

  const startTs = events[0].timestamp;
  const result: ImportantEvent[] = [];

  let phase: Phase = "pending";
  let prevInstruction: string | undefined;
  let prevWaypointCount: number | undefined;
  let prevStateUpdateTs: number | undefined;
  let prevOffRoute = false;

  for (const e of events) {
    const offsetMs = e.timestamp - startTs;
    const push = (type: string, label: string, tagClass: string): void => {
      result.push({ offsetMs, type, label, tagClass });
    };

    if ("RouteUpdate" in e.event_data) {
      push("Reroute", "Route updated", "is-warning");
      continue;
    }
    if (!("StateUpdate" in e.event_data)) continue;

    const { trip_state: tripState } = e.event_data.StateUpdate;

    if (phase === "navigating" && prevStateUpdateTs !== undefined) {
      const gap = e.timestamp - prevStateUpdateTs;
      if (gap > GPS_GAP_THRESHOLD_MS) {
        push(
          "Signal lost",
          `No updates for ${Math.round(gap / 1000)}s`,
          "is-warning",
        );
      }
    }
    prevStateUpdateTs = e.timestamp;

    if ("Navigating" in tripState) {
      const { Navigating: nav } = tripState;

      if (phase === "pending") {
        push("Start", "Navigation started", "is-info");
      }
      phase = "navigating";

      const instruction = nav.remainingSteps?.[0]?.instruction;
      if (instruction && instruction !== prevInstruction) {
        if (prevInstruction !== undefined) {
          push("Step", instruction, "is-light");
        }
        prevInstruction = instruction;
      }

      const waypointCount = nav.remainingWaypoints?.length ?? 0;
      if (
        prevWaypointCount !== undefined &&
        waypointCount < prevWaypointCount
      ) {
        push("Waypoint", "Waypoint reached", "is-success");
      }
      prevWaypointCount = waypointCount;

      const offRoute =
        typeof nav.deviation === "object" &&
        nav.deviation !== null &&
        "OffRoute" in nav.deviation;
      if (offRoute && !prevOffRoute) {
        push("Off route", "Deviated from route", "is-danger");
      } else if (!offRoute && prevOffRoute) {
        push("On route", "Returned to route", "is-success");
      }
      prevOffRoute = offRoute;
    } else if ("Complete" in tripState && phase !== "complete") {
      push("Arrived", "Reached destination", "is-success");
      phase = "complete";
    } else if ("Idle" in tripState && phase === "navigating") {
      push("Stopped", "Navigation ended", "is-danger");
      phase = "ended";
    }
  }

  return result;
};
