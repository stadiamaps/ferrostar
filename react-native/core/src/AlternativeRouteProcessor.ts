import type { Route } from "ferrostar-rn-uniffi";
import type { FerrostarCore } from "./FerrostarCore";

/** Reacts to the core loading alternative routes. */
export interface AlternativeRouteProcessor {
  /**
   * A hook for the developer can decide whether to act on new routes becoming available. This is
   * currently used for recalculation when the user diverges from the route, but can be extended for
   * other uses in the future.
   */
  loadedAlternativeRoutes(core: FerrostarCore, routes: Array<Route>): void;
}
