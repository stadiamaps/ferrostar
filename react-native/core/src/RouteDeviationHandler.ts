import type {
  DeviationKind,
  Waypoint,
} from '@stadiamaps/ferrostar-uniffi-react-native';
import type { FerrostarCore } from './FerrostarCore';

export enum CorrectiveAction {
  DoNothing,
  GetNewRoutes,
}

export interface RouteDeviationHandler {
  correctiveActionForDeviation(
    core: FerrostarCore,
    deviation: DeviationKind,
    remainingWaypoints: Array<Waypoint>
  ): CorrectiveAction;
}
