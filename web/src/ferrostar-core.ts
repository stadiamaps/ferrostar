import { LitElement, PropertyValues } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import {
  JsNavState,
  NavigationController,
  RouteAdapter,
  TripState,
} from "@stadiamaps/ferrostar";
import { SimulatedLocationProvider } from "./location";

/**
 * A core navigation component that handles navigation logic without UI.
 */
@customElement("ferrostar-core")
export class FerrostarCore extends LitElement {
  @property()
  valhallaEndpointUrl: string = "";

  @property()
  profile: string = "";

  @property({ attribute: false })
  httpClient?: Function = fetch;

  @property({ type: Object, attribute: false })
  locationProvider!: any;

  @property({ type: Object, attribute: false })
  options: object = {};

  @state()
  protected _navState: JsNavState | null = null;

  @property({ type: Function, attribute: false })
  onNavigationStart?: () => void;

  @property({ type: Function, attribute: false })
  onNavigationStop?: () => void;

  @property({ type: Function, attribute: false })
  onTripStateChange?: (newState: TripState | null) => void;

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

  async getRoutes(initialLocation: any, waypoints: any) {
    this.routeAdapter = new RouteAdapter(
      this.valhallaEndpointUrl,
      this.profile,
      JSON.stringify(this.options),
    );

    const routeRequest = this.routeAdapter.generateRequest(
      initialLocation,
      waypoints,
    );
    const method = routeRequest.get("method");
    let url = new URL(routeRequest.get("url"));
    const body = routeRequest.get("body");

    const response = await this.httpClient!(url, {
      method: method,
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

  startNavigation(route: any, config: any) {
    this.locationProvider.start();
    if (this.onNavigationStart) this.onNavigationStart();

    this.navigationController = new NavigationController(
      route,
      config,
      this.shouldRecord,
    );
    this.locationProvider.updateCallback = this.onLocationUpdated.bind(this);

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

  async stopNavigation() {
    this.routeAdapter?.free();
    this.routeAdapter = null;
    this.navigationController?.free();
    this.navigationController = null;
    this.navStateUpdate(null);
    if (this.locationProvider) this.locationProvider.updateCallback = null;
    if (this.onNavigationStop) this.onNavigationStop();
  }

  private navStateUpdate(newState: JsNavState | null) {
    this._navState = newState;
    this.onTripStateChange?.(newState?.tripState || null);

    // Dispatch event for external listeners
    this.dispatchEvent(
      new CustomEvent("navState-change", {
        detail: { tripState: newState || null },
        bubbles: true,
      }),
    );
  }

  private onLocationUpdated() {
    if (!this.navigationController) {
      return;
    }

    const newNavState = this.navigationController.updateUserLocation(
      this.locationProvider.lastLocation,
      this._navState,
    );
    this.navStateUpdate(newNavState);

    // Handle voice guidance
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

  get navState(): JsNavState | null {
    return this._navState;
  }

  // This component doesn't render anything - it's purely for logic
  render() {
    return null;
  }
}
