import {css, html, LitElement, PropertyValues, unsafeCSS} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import maplibregl, {LngLatLike} from "maplibre-gl";
import maplibreglStyles from "maplibre-gl/dist/maplibre-gl.css?inline";
import {MapLibreSearchControl} from "@stadiamaps/maplibre-search-box";
import searchBoxStyles
  from "@stadiamaps/maplibre-search-box/dist/style.css?inline";
import {NavigationController, RouteAdapter} from "@stadiamaps/ferrostar";
import "./instructions-view";
import "./arrival-view";
import {BrowserLocationProvider, SimulatedLocationProvider} from "./location";
import CloseSvg from "./assets/directions/close.svg";

@customElement("ferrostar-map")
export class FerrostarMap extends LitElement {
  @property()
  valhallaEndpointUrl: string = "";

  @property()
  styleUrl: string = "";

  @property()
  profile: string = "";

  @property()
  center: LngLatLike | null = null;

  @property()
  pitch: number = 60;

  @property()
  zoom: number = 6;

  @property({ attribute: false })
  httpClient?: Function = fetch;

  // TODO: type
  @property({ type: Object })
  locationProvider!: any;

  // TODO: type
  @property({ type: Object })
  costingOptions: object = {};

  // TODO: type
  @state()
  protected _tripState: any = null;

  @property({ type: Boolean })
  useIntegratedSearchBox: boolean = false;

  @property({ type: Boolean })
  useVoiceGuidance: boolean = false;

  routeAdapter: RouteAdapter | null = null;
  map: maplibregl.Map | null = null;
  searchBox: MapLibreSearchControl | null = null;
  navigationController: NavigationController | null = null;
  currentLocationMapMarker: maplibregl.Marker | null = null;
  lastSpokenInstructionText: string | null = null;

  static styles = [
    unsafeCSS(maplibreglStyles),
    unsafeCSS(searchBoxStyles),
    css`
      [hidden] {
        display: none !important;
      }

      #map {
        height: 100%;
        width: 100%;
      }

      instructions-view {
        top: 10px;
        position: absolute;
        left: 50%;
        transform: translateX(-50%);
        max-width: 80%;
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
        transition: background-color 0.3s, filter 0.3s;
      }

      #stop-button .icon {
        width: 20px;
        height: 20px;
      }

      #stop-button:hover {
        background-color: #e0e0e0;
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
    if (this.map) {
      if (changedProperties.has("styleUrl")) {
        this.map.setStyle(this.styleUrl)
      }
      if (changedProperties.has("center")) {
        if (changedProperties.get("center") === null && this.center !== null) {
          this.map.jumpTo({center: this.center})
        } else if (this.center !== null) {
          this.map.flyTo({center: this.center})
        }
      }
      if (changedProperties.has("pitch")) {
        this.map.setPitch(this.pitch)
      }
      if (changedProperties.has("zoom")) {
        this.map.setZoom(this.zoom)
      }
    }
  }

  firstUpdated() {
    this.map = new maplibregl.Map({
      container: this.shadowRoot!.getElementById("map")!,
      style: this.styleUrl ? this.styleUrl : "https://demotiles.maplibre.org/style.json",
      center: this.center ?? [0, 0],
      pitch: this.pitch,
      bearing: 0,
      zoom: this.zoom,
    });

    if (this.useIntegratedSearchBox) {
      this.searchBox = new MapLibreSearchControl({
        onResultSelected: async (feature) => {
          await this.startNavigationFromSearch(feature.geometry.coordinates);
        },
      });
      this.map.addControl(this.searchBox, "top-left");
    }
  }

  // TODO: type
  async getRoutes(initialLocation: any, waypoints: any) {
    // Initialize the route adapter
    this.routeAdapter = new RouteAdapter(this.valhallaEndpointUrl, this.profile, JSON.stringify(this.costingOptions));

    // Generate the request body
    const body = this.routeAdapter.generateRequest(initialLocation, waypoints).get("body");

    // Send the request to the Valhalla endpoint
    // FIXME: assert httpClient is not null
    const response = await this.httpClient!(this.valhallaEndpointUrl, {
      method: "POST",
      // FIXME: assert body is not null
      body: new Uint8Array(body).buffer,
    });

    const responseData = new Uint8Array(await response.arrayBuffer());
    return this.routeAdapter.parseResponse(responseData);
  }

  // TODO: type
  startNavigation(route: any, config: any) {
    // Remove the search box when navigation starts
    if (this.useIntegratedSearchBox) this.map?.removeControl(this.searchBox!);

    // Initialize the navigation controller
    this.navigationController = new NavigationController(route, config);
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

    this._tripState = this.navigationController.getInitialState(startingLocation);

    // Update the UI with the initial trip state
    this.clearMap();

    this.map?.addSource("route", {
      type: "geojson",
      data: {
        type: "Feature",
        properties: {},
        geometry: {
          type: "LineString",
          coordinates: route.geometry.map((point: { lat: number; lng: number }) => [point.lng, point.lat]),
        },
      },
    });

    this.map?.addLayer({
      id: "route",
      type: "line",
      source: "route",
      layout: {
        "line-join": "round",
        "line-cap": "round",
      },
      paint: {
        "line-color": "#3700B3",
        "line-width": 8,
      },
    });

    this.map?.setCenter(route.geometry[0]);

    this.currentLocationMapMarker = new maplibregl.Marker().setLngLat(route.geometry[0]).addTo(this.map!);
  }

  async startNavigationFromSearch(coordinates: any) {
    const waypoints = [{ coordinate: { lat: coordinates[1], lng: coordinates[0] }, kind: "Break" }];

    // FIXME: This is a hack basically to support the demo page that should go away.
    if (!this.locationProvider || this.locationProvider instanceof SimulatedLocationProvider) {
      this.locationProvider = new BrowserLocationProvider();
    }

    this.locationProvider.start();

    // TODO: Replace this with a promise or callback
    while (!this.locationProvider.lastLocation) {
      await new Promise((resolve) => setTimeout(resolve, 100));
    }

    // Use the acquired user location to request the route
    const routes = await this.getRoutes(this.locationProvider.lastLocation, waypoints);
    const route = routes[0];

    // TODO: type + use TypeScript enum
    const config = {
      stepAdvance: {
        RelativeLineStringDistance: {
          minimumHorizontalAccuracy: 25,
          automaticAdvanceDistance: 10,
        },
      },
      routeDeviationTracking: {
        StaticThreshold: {
          minimumHorizontalAccuracy: 25,
          maxAcceptableDeviation: 10.0,
        },
      },
    };

    // Start the navigation
    this.startNavigation(route, config);
  }

  async stopNavigation() {
    // TODO: Factor out the UI layer from the core
    this.clearMap();
    this.routeAdapter?.free();
    this.routeAdapter = null;
    this.navigationController?.free();
    this.navigationController = null;
    this._tripState = null;
    if (this.locationProvider) this.locationProvider.updateCallback = null;
    if (this.useIntegratedSearchBox) this.map?.addControl(this.searchBox!, "top-left");
  }

  private onLocationUpdated() {
    if (!this.navigationController) {
      return;
    }
    // Update the trip state with the new location
    this._tripState = this.navigationController!.updateUserLocation(this.locationProvider.lastLocation, this._tripState);

    // Update the user's location on the map
    this.currentLocationMapMarker?.setLngLat(this.locationProvider.lastLocation.coordinates);

    // Center the map on the user's location
    this.map?.easeTo({
      center: this.locationProvider.lastLocation.coordinates,
      bearing: this.locationProvider.lastLocation.courseOverGround.degrees || 0,
    });

    // Speak the next instruction if voice guidance is enabled
    if (this.useVoiceGuidance) {
      if (this._tripState.Navigating?.spokenInstruction && this._tripState.Navigating?.spokenInstruction.text !== this.lastSpokenInstructionText) {
        this.lastSpokenInstructionText = this._tripState.Navigating?.spokenInstruction.text;
        window.speechSynthesis.cancel();
        window.speechSynthesis.speak(new SpeechSynthesisUtterance(this._tripState.Navigating?.spokenInstruction.text));
      }
    }
  }

  private clearMap() {
    this.map?.getLayer("route") && this.map?.removeLayer("route");
    this.map?.getSource("route") && this.map?.removeSource("route");
    this.currentLocationMapMarker?.remove();
  }

  render() {
    return html`
      <div id="map">
        <instructions-view .tripState=${this._tripState}></instructions-view>
        <div id="bottom-component">
          <arrival-view .tripState=${this._tripState}></arrival-view>
          <button id="stop-button" @click=${this.stopNavigation} ?hidden=${!this._tripState}>
            <img src=${CloseSvg} alt="Stop navigation" class="icon" />
          </button>
        </div>
      </div>
    `;
  }
}
