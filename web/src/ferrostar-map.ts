import { css, html, LitElement, PropertyValues } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import maplibregl, { GeolocateControl, Map } from "maplibre-gl";
import { NavigationController, RouteAdapter, SerializableNavState, TripState } from "@stadiamaps/ferrostar";
import "./instructions-view";
import "./trip-progress-view";
import { SimulatedLocationProvider } from "./location";
import CloseSvg from "./assets/directions/close.svg";

/**
 * A MapLibre-based map component specialized for navigation.
 */
@customElement("ferrostar-map")
export class FerrostarMap extends LitElement {
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
  onNavigationStart?: (map: Map) => void;

  @property({ type: Function, attribute: false })
  onNavigationStop?: (map: Map) => void;

  @property({ type: Function, attribute: true })
  onTripStateChange?: (newState: TripState | null) => void;

  /**
   *  Styles to load which will apply inside the component
   */
  @property({ type: Object, attribute: false })
  customStyles?: object | null;

  /**
   * A boolean flag indicating whether recording should occur.
   *
   * When set to `true`, the system will perform recording operations.
   * When set to `false`, recording operations are disabled.
   */
  @property({ type: Boolean })
  should_record: boolean = false;

  /**
   * Enables voice guidance via the web speech synthesis API.
   * Defaults to false.
   */
  @property({ type: Boolean })
  useVoiceGuidance: boolean = false;

  /**
   * Automatically geolocates the user on map load.
   *
   * Defaults to true.
   * Has no effect if `addGeolocateControl` is false.
   */
  @property({ type: Boolean })
  geolocateOnLoad: boolean = true;

  /**
   * Optionally adds a geolocate control to the map.
   *
   * Defaults to true.
   * Set this to false if you want to disable the geolocation control or bring your own.
   */
  @property({ type: Boolean })
  addGeolocateControl: boolean = true;

  routeAdapter: RouteAdapter | null = null;

  /**
   * The MapLibre map instance.
   *
   * You have to explicitly set this value when initializing
   * the web component to provide your own map instance.
   *
   */
  @property({ type: Object })
  map!: maplibregl.Map;

  geolocateControl: GeolocateControl | null = null;
  navigationController: NavigationController | null = null;
  simulatedLocationMarker: maplibregl.Marker | null = null;
  lastSpokenUtteranceId: string | null = null;

  static styles = [
    css`
      [hidden] {
        display: none !important;
      }

      #container {
        height: 100%;
        width: 100%;
      }

      #map,
      ::slotted(:first-child) {
        height: 100%;
        width: 100%;
        display: block;
      }

      instructions-view {
        top: 10px;
        position: absolute;
        left: 50%;
        transform: translateX(-50%);
        width: 80%;
        z-index: 1000;
      }

      #bottom-component {
        bottom: 10px;
        position: absolute;
        left: 50%;
        transform: translateX(-50%);
        max-width: 80%;
        z-index: 1000;
        display: flex;
        justify-content: space-between;
        gap: 10px;
      }

      #stop-button {
        display: flex;
        padding: 20px;
        background-color: white;
        border-radius: 50%;
        border: none;
        box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
        cursor: pointer;
        transition:
          background-color 0.3s,
          filter 0.3s;
      }

      #stop-button .icon {
        width: 20px;
        height: 20px;
      }

      #stop-button:hover {
        background-color: #e0e0e0;
      }

      @media (max-width: 600px) {
        #stop-button {
          padding: 14px;
        }

        #stop-button .icon {
          width: 10px;
          height: 10px;
        }
      }
    `,
  ];

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

  firstUpdated() {
    if (this.addGeolocateControl) {
      this.geolocateControl = new GeolocateControl({
        positionOptions: {
          enableHighAccuracy: true,
        },
        trackUserLocation: true,
      });

      this.map.addControl(this.geolocateControl);

      this.map.on("load", (_) => {
        if (this.geolocateOnLoad) {
          this.geolocateControl?.trigger();
        }
      });
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
    if (this.onNavigationStart && this.map) this.onNavigationStart(this.map);

    // Initialize the navigation controller
    this.navigationController = new NavigationController(
      route,
      config,
      this.should_record,
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

    // Update the UI with the initial trip state
    this.clearMap();

    this.map?.addSource("route", {
      type: "geojson",
      data: {
        type: "Feature",
        properties: {},
        geometry: {
          type: "LineString",
          coordinates: route.geometry.map(
            (point: { lat: number; lng: number }) => [point.lng, point.lat],
          ),
        },
      },
    });

    // TODO: Configuration param where to insert the layer
    this.map?.addLayer({
      id: "route",
      type: "line",
      source: "route",
      layout: {
        "line-join": "round",
        "line-cap": "round",
      },
      paint: {
        "line-color": "#3478f6",
        "line-width": 8,
      },
    });

    this.map?.addLayer(
      {
        id: "route-border",
        type: "line",
        source: "route",
        layout: {
          "line-join": "round",
          "line-cap": "round",
        },
        paint: {
          "line-color": "#FFFFFF",
          "line-width": 13,
        },
      },
      "route",
    );

    this.map?.flyTo({
      center: route.geometry[0],
    });

    if (this.locationProvider instanceof SimulatedLocationProvider) {
      this.simulatedLocationMarker = new maplibregl.Marker({
        color: "green",
      })
        .setLngLat(route.geometry[0])
        .addTo(this.map!);
    }
  }

  async stopNavigation() {
    // TODO: Factor out the UI layer from the core
    this.routeAdapter?.free();
    this.routeAdapter = null;

    if (this.should_record) {
      let recording = this.navigationController?.get_recording(
        this._navState?.recordingEvents,
      );
      this.saveJsonStringToFile("recording.json", recording);
    }

    this.navigationController?.free();
    this.navigationController = null;
    this.navStateUpdate(null);
    this.clearMap();
    if (this.locationProvider) this.locationProvider.updateCallback = null;
    if (this.onNavigationStop && this.map) this.onNavigationStop(this.map);
  }

  private saveJsonStringToFile(filename: string, jsonString: string) {
    console.log(jsonString);
    const blob = new Blob([jsonString], { type: "application/json" });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = filename;
    link.click();
    URL.revokeObjectURL(link.href);
  }

  private navStateUpdate(newState: SerializableNavState | null) {
    this._navState = newState;
    this.onTripStateChange?.(newState?.tripState || null);

    if (newState?.tripState && "Complete" in newState.tripState) {
      this.stopNavigation();
    }
  }

  private onLocationUpdated() {
    if (!this.navigationController) {
      return;
    }
    // Update the trip state with the new location
    const newNavState = this.navigationController!.updateUserLocation(
      this.locationProvider.lastLocation,
      this._navState,
    );
    this.navStateUpdate(newNavState);

    // Update the simulated location marker if needed
    this.simulatedLocationMarker?.setLngLat(
      this.locationProvider.lastLocation.coordinates,
    );

    // Center the map on the user's location
    this.map?.easeTo({
      center: this.locationProvider.lastLocation.coordinates,
      bearing: this.locationProvider.lastLocation.courseOverGround.degrees || 0,
    });

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

  private clearMap() {
    this.map?.getLayer("route") && this.map?.removeLayer("route");
    this.map?.getLayer("route-border") && this.map?.removeLayer("route-border");
    this.map?.getSource("route") && this.map?.removeSource("route");
    this.simulatedLocationMarker?.remove();
  }

  render() {
    return html`
      <style>
        ${this.customStyles}
      </style>
      <div id="container">
        <div id="map">
          <!-- Fix names/ids; currently this is a breaking change -->
          <div id="overlay">
            <instructions-view
              .tripState=${this._navState?.tripState}
            ></instructions-view>

            <div id="bottom-component">
              <trip-progress-view
                .tripState=${this._navState?.tripState}
              ></trip-progress-view>
              <button
                id="stop-button"
                @click=${this.stopNavigation}
                ?hidden=${!this._navState?.tripState}
              >
                <img src=${CloseSvg} alt="Stop navigation" class="icon" />
              </button>
            </div>
          </div>
        </div>
      </div>
    `;
  }
}
