import type { Waypoint } from '../generated/ferrostar';
import type { FerrostarCore } from './FerrostarCore';

export enum CorrectiveAction {
  DoNothing,
  GetNewRoutes,
}

export interface RouteDeviationHandler {
  correctiveActionForDeviation(
    core: FerrostarCore,
    deviationInMeters: number,
    remainingWaypoints: Array<Waypoint>
  ): CorrectiveAction;
}
