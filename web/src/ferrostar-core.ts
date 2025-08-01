import { LitElement, PropertyValues } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import {
  SerializableNavState,
  NavigationController,
  RouteAdapter,
  TripState,
} from "@stadiamaps/ferrostar";
import { StateProvider } from "./types";

/**
 * A core navigation component that handles navigation logic without UI.
 */
@customElement("ferrostar-core")
export class FerrostarCore extends LitElement implements StateProvider {
  @property()
  valhallaEndpointUrl: string = "";

  @property()
  profile: string = "";

  @property({ attribute: false })
  httpClient?: Function = fetch;

  // TODO: type
  @property({ type: Object, attribute: false })
  locationProvider!: any;

  // TODO: type
  @property({ type: Object, attribute: false })
  options: object = {};

  @state()
  protected _navState: SerializableNavState | null = null;

  @property({ type: Function, attribute: false })
  onNavigationStart?: () => void;

  @property({ type: Function, attribute: false })
  onNavigationStop?: () => void;

  @property({ type: Function, attribute: false })
  onTripStateChange?: (newState: TripState | null) => void;

  /**
   * Enables voice guidance via the web speech synthesis API.
   * Defaults to false.
   */
  @property({ type: Boolean })
  useVoiceGuidance: boolean = false;

  @property({ type: Boolean })
  shouldRecord: boolean = false;

  routeAdapter: RouteAdapter | null = null;
  navigationController: NavigationController | null = null;
  lastSpokenUtteranceId: string | null = null;

  constructor() {
    super();

    // A workaround for avoiding "Illegal invocation"
    if (this.httpClient === fetch) {
      this.httpClient = this.httpClient.bind(window);
    }
  }

  updated(changedProperties: PropertyValues<this>) {
    if (changedProperties.has("locationProvider") && this.locationProvider) {
      this.locationProvider.updateCallback = this.onLocationUpdated.bind(this);
    }
  }

  // TODO: type
  async getRoutes(initialLocation: any, waypoints: any) {
    // Initialize the route adapter
    // (NOTE: currently only supports Valhalla, but working toward expansion)
    this.routeAdapter = new RouteAdapter(
      this.valhallaEndpointUrl,
      this.profile,
      JSON.stringify(this.options),
    );

    // Generate the request body
    const routeRequest = this.routeAdapter.generateRequest(
      initialLocation,
      waypoints,
    );
    const method = routeRequest.get("method");
    let url = new URL(routeRequest.get("url"));
    const body = routeRequest.get("body");

    // Send the request to the Valhalla endpoint
    // FIXME: assert httpClient is not null
    const response = await this.httpClient!(url, {
      method: method,
      // FIXME: assert body is not null
      body: new Uint8Array(body).buffer,
    });

    const responseData = new Uint8Array(await response.arrayBuffer());
    try {
      return this.routeAdapter.parseResponse(responseData);
    } catch (e) {
      console.error("Error parsing route response:", e);
      throw e;
    }
  }

  // TODO: types
  startNavigation(route: any, config: any) {
    this.locationProvider.start();
    if (this.onNavigationStart) this.onNavigationStart();

    // Initialize the navigation controller
    this.navigationController = new NavigationController(
      route,
      config,
      this.shouldRecord,
    );
    this.locationProvider.updateCallback = this.onLocationUpdated.bind(this);

    // Initialize the trip state
    const startingLocation = this.locationProvider.lastLocation
      ? this.locationProvider.lastLocation
      : {
          coordinates: route.geometry[0],
          horizontalAccuracy: 0.0,
          courseOverGround: null,
          timestamp: Date.now(),
          speed: null,
        };

    this.navStateUpdate(
      this.navigationController.getInitialState(startingLocation),
    );
  }

  private saveRecording() {
    let recording = this.navigationController?.getRecording(
      this._navState?.recordingEvents,
    );
    const blob = new Blob([recording], { type: "application/json" });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    // TODO: Figure out how to generate a unique filename
    link.download = "recording.json";
    link.click();
    URL.revokeObjectURL(link.href);
  }

  async stopNavigation() {
    if (this.shouldRecord) this.saveRecording();
    this.routeAdapter?.free();
    this.routeAdapter = null;
    this.navigationController?.free();
    this.navigationController = null;
    this.navStateUpdate(null);
    if (this.locationProvider) this.locationProvider.updateCallback = null;
    if (this.onNavigationStop) this.onNavigationStop();
  }

  provideState(tripState: TripState) {
    // Dispatch event for external listeners
    this.dispatchEvent(
      new CustomEvent("tripstate-update", {
        detail: { tripState },
        bubbles: true,
      }),
    );
  }

  private navStateUpdate(newState: SerializableNavState | null) {
    this._navState = newState;
    this.onTripStateChange?.(newState?.tripState || null);
    if (!newState) return;

    this.provideState(newState.tripState);
  }

  private onLocationUpdated() {
    if (!this.navigationController) {
      return;
    }

    // Update the trip state with the new location
    const newNavState = this.navigationController.updateUserLocation(
      this.locationProvider.lastLocation,
      this._navState,
    );
    this.navStateUpdate(newNavState);

    // Speak the next instruction if voice guidance is enabled
    const tripState = this._navState?.tripState;
    if (
      this.useVoiceGuidance &&
      tripState != null &&
      typeof tripState === "object"
    ) {
      if (
        "Navigating" in tripState &&
        tripState.Navigating?.spokenInstruction &&
        tripState.Navigating?.spokenInstruction.utteranceId !==
          this.lastSpokenUtteranceId
      ) {
        this.lastSpokenUtteranceId =
          tripState.Navigating?.spokenInstruction.utteranceId;
        window.speechSynthesis.cancel();
        window.speechSynthesis.speak(
          new SpeechSynthesisUtterance(
            tripState.Navigating?.spokenInstruction.text,
          ),
        );
      }
    }
  }

  // This component doesn't render anything - it's purely for logic
  render() {
    return null;
  }
}
