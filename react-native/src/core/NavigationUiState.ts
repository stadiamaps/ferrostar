import {
  TripState,
  type GeographicCoordinate,
  type RouteDeviation,
  type RouteStep,
  type SpokenInstruction,
  type TripProgress,
  type UserLocation,
  type VisualInstruction,
} from '../generated/ferrostar';
import type { NavigationState } from './FerrostarCore';

export class NavigationUiState {
  /** The user's location as reported by the location provider. */
  location?: UserLocation;
  /** The user's location snapped to the route shape. */
  snappedLocation?: UserLocation;
  /**
   * The last known heading of the user.
   *
   * NOTE: This is distinct from the course over ground (direction of travel), which is included
   * in the `location` and `snappedLocation` properties.
   */
  heading?: number;
  /** The geometry of the full route. */
  routeGeometry?: Array<GeographicCoordinate>;
  /** Visual instructions which should be displayed based on the user's current progress. */
  visualInstruction?: VisualInstruction;
  /**
   * Instructions which should be spoken via speech synthesis based on the user's current
   * progress.
   */
  spokenInstruction?: SpokenInstruction;
  /** The user's progress through the current trip. */
  progress?: TripProgress;
  /** If true, the core is currently calculating a new route. */
  isCalculatingNewRoute?: boolean;
  /** Describes whether the user is believed to be off the correct route. */
  routeDeviation?: RouteDeviation;
  /** If true, spoken instructions will not be synthesized. */
  isMuted?: boolean;
  /** The name of the road which the current route step is traversing. */
  currentStepRoadName?: string;
  /** The remaining steps in the trip (including the current step). */
  remainingSteps?: Array<RouteStep>;
  /** The route annotation object at the current location. */
  // TODO: Annotation implementation
  //currentAnnotation: AnnotationWrapper<*>

  constructor(
    location?: UserLocation,
    snappedLocation?: UserLocation,
    heading?: number,
    routeGeometry?: Array<GeographicCoordinate>,
    visualInstruction?: VisualInstruction,
    spokenInstruction?: SpokenInstruction,
    progress?: TripProgress,
    isCalculatingNewRoute?: boolean,
    routeDeviation?: RouteDeviation,
    isMuted?: boolean,
    currentStepRoadName?: string,
    remainingSteps?: Array<RouteStep>
  ) {
    this.location = location;
    this.snappedLocation = snappedLocation;
    this.heading = heading;
    this.routeGeometry = routeGeometry;
    this.visualInstruction = visualInstruction;
    this.spokenInstruction = spokenInstruction;
    this.progress = progress;
    this.isCalculatingNewRoute = isCalculatingNewRoute;
    this.routeDeviation = routeDeviation;
    this.isMuted = isMuted;
    this.currentStepRoadName = currentStepRoadName;
    this.remainingSteps = remainingSteps;
  }

  setMuted(isMuted: boolean): NavigationUiState {
    this.isMuted = isMuted;
    return this;
  }

  static fromFerrostar(
    coreState: NavigationState,
    isMuted?: boolean,
    location?: UserLocation,
    snappedLocation?: UserLocation
  ): NavigationUiState {
    let tripState;
    if (TripState.Navigating.instanceOf(coreState.tripState)) {
      tripState = coreState.tripState;
    }
    return new NavigationUiState(
      location,
      snappedLocation ?? tripState?.inner.snappedUserLocation,
      undefined,
      coreState.routeGeometry,
      tripState?.inner.visualInstruction,
      undefined,
      tripState?.inner.progress,
      coreState.isCalculatingNewRoute,
      tripState?.inner.deviation,
      isMuted,
      undefined, // TODO: Android seems to have this type but it's not in the TS definition
      tripState?.inner.remainingSteps
    );
  }

  isNavigating(): boolean {
    return this.progress !== undefined;
  }
}
