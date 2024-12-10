import { NativeSyntheticEvent } from "react-native";

export type GeographicCoordinate = {
  lat: number;
  lng: number;
};

export type BoundingBox = {
  ne: GeographicCoordinate;
  sw: GeographicCoordinate;
};

export type Waypoint = {
  coordinate: GeographicCoordinate;
  kind: "break" | "via";
};

export type LaneInfo = {
  active: boolean;
  directions: string[];
  activeDirection?: string;
};

export type VisualInstructionContent = {
  text: string;
  maneuverType?:
    | "turn"
    | "new_name"
    | "depart"
    | "arrive"
    | "merge"
    | "on_ramp"
    | "off_ramp"
    | "fork"
    | "end_of_road"
    | "continue"
    | "roundabout"
    | "notification"
    | "exit_roundabout"
    | "exit_rotary";
  maneuverModifier?:
    | "u_turn"
    | "sharp_right"
    | "right"
    | "slight_right"
    | "straight"
    | "slight_left"
    | "left"
    | "sharp_left";
  roundaboutExitDegrees?: number;
  laneInfo?: LaneInfo[];
};

export type VisualInstruction = {
  primaryContent: VisualInstructionContent;
  secondaryContent?: VisualInstructionContent;
  subContent?: VisualInstructionContent;
  triggerDistanceBeforeManeuver: number;
};

export type SpokenInstruction = {
  text: string;
  ssml?: string;
  triggerDistanceBeforeManeuver: number;
  utteranceId: string;
};

export type RouteStep = {
  geometry: GeographicCoordinate[];
  distance: number;
  duration: number;
  roadName?: string;
  instruction: string;
  visualInstructions: VisualInstruction[];
  spokenInstructions: SpokenInstruction[];
  annotation: string[];
};

export type Route = {
  geometry: GeographicCoordinate[];
  bbox: BoundingBox;
  distance: number;
  waypoints: Waypoint[];
  steps: RouteStep[];
};

export type Speed = {
  value: number;
  accuracy?: number;
};

export type UserLocation = {
  coordinates: GeographicCoordinate;
  horizontalAccuracy: number;
  courseOverGround?: CourseOverGround;
  timestamp: string;
  speed?: Speed;
};

export type CourseOverGround = {
  degrees: number;
  accuracy?: number;
};

export type RelativeLineStringDistance = {
  minimumHorizontalAccuracy: number;
  automaticAdvanceDistance?: number;
};

export type StaticThreshold = {
  minimumHorizontalAccuracy: number;
  maxAcceptableDeviation: number;
};

export type NavigationControllerConfig = {
  stepAdvance: RelativeLineStringDistance;
  routeDeviationTracking: StaticThreshold;
  courseFiltering: "SNAP_TO_ROUTE" | "RAW";
};

export type CoreOptions = {
  locationMode?: "fused" | "default" | "simulated";
  valhallaEndpointURL?: string;
  profile?: string;
  options?: object;
  navigationControllerConfig?: NavigationControllerConfig;
};

export type NavigationOptions = {
  styleUrl?: string;
  snapUserLocationToRoute?: boolean;
};

export type NativeViewProps = {
  navigationOptions?: NavigationOptions;
  coreOptions?: CoreOptions;
};

export type FerrostarViewProps = { id?: string } & NavigationOptions &
  CoreOptions;

export type ExpoFerrostarModule = {
  createRouteFromOsrm: (
    route: string,
    waypoints: string
  ) => Promise<Route | null>;
  startNavigation: (route: Route, options?: NavigationControllerConfig) => void;
  stopNavigation: (stopLocationUpdates?: boolean) => void;
  replaceRoute: (route: Route, options?: NavigationControllerConfig) => void;
  advanceToNextStep: () => void;
  getRoutes: (
    initialLocation: UserLocation,
    waypoints: Waypoint[]
  ) => Promise<Route[]>;
};

export type NavigationStateChangeEvent = NativeSyntheticEvent<NavigationState>;

export type NavigationState = {
  isNavigating: boolean;
  isCalculatingNewRoute: boolean;
};
