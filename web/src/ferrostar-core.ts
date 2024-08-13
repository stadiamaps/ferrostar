import { LitElement, html, css, unsafeCSS } from "lit";
import { customElement, property } from "lit/decorators.js";
import maplibregl from "maplibre-gl";
import maplibreglStyles from "maplibre-gl/dist/maplibre-gl.css?inline";
import { MapLibreSearchControl } from "@stadiamaps/maplibre-search-box";
import searchBoxStyles from "@stadiamaps/maplibre-search-box/dist/style.css?inline";
import init, { NavigationController, RouteAdapter } from "ferrostar";
import "./instructions-view";
import "./arrival-view";
import { BrowserLocationProvider } from "./location";

@customElement("ferrostar-core")
export class FerrostarCore extends LitElement {
  @property()
  valhallaEndpointUrl: string = "";

  @property()
  styleUrl: string = "";

  @property()
  profile: string = "";

  @property({ attribute: false })
  httpClient?: Function = fetch;

  // TODO: type
  @property({ type: Object })
  locationProvider!: any;

  // TODO: type
  @property({ type: Object })
  costingOptions!: any;

  // TODO: type
  @property({ type: Object })
  tripState: any = null;

  // TODO: type
  @property({ type: Boolean })
  useIntegratedSearchBox: boolean = true;

  routeAdapter: RouteAdapter | null = null;
  map: maplibregl.Map | null = null;
  searchBox: MapLibreSearchControl | null = null;
  navigationController: NavigationController | null = null;
  currentLocationMapMarker: maplibregl.Marker | null = null;

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

  updated(changedProperties: any) {
    if (changedProperties.has("locationProvider") && this.locationProvider) {
      this.locationProvider.updateCallback = this.onLocationUpdated.bind(this);
    }
  }

  firstUpdated() {
    this.map = new maplibregl.Map({
      container: this.shadowRoot!.getElementById("map")!,
      style: this.styleUrl ? this.styleUrl : "https://demotiles.maplibre.org/style.json",
      center: [-122.42, 37.81],
      pitch: 60,
      bearing: 0,
      zoom: 18,
    });

    if (this.useIntegratedSearchBox) {
      this.searchBox = new MapLibreSearchControl({
        onResultSelected: (feature) => {
          this.startNavigationFromSearch(feature.geometry.coordinates);
        },
      });
      this.map.addControl(this.searchBox, "bottom-left");
    }
  }

  // TODO: type
  async getRoutes(initialLocation: any, waypoints: any) {
    await init();

    this.routeAdapter = new RouteAdapter(this.valhallaEndpointUrl, this.profile);

    const body = this.routeAdapter.generateRequest(initialLocation, waypoints).get("body");

    // FIXME: assert httpClient is not null
    const response = await this.httpClient!(this.valhallaEndpointUrl, {
      method: "POST",
      // FIXME: assert body is not null
      body: new Uint8Array(body).buffer,
    });

    const responseData = new Uint8Array(await response.arrayBuffer());
    const routes = this.routeAdapter.parseResponse(responseData);

    return routes;
  }

  // TODO: type
  async startNavigation(route: any, config: any) {
    if (this.useIntegratedSearchBox) this.map?.removeControl(this.searchBox!);

    this.locationProvider.updateCallback = this.onLocationUpdated.bind(this);
    this.navigationController = new NavigationController(route, config);

    const startingLocation = this.locationProvider.lastLocation
      ? this.locationProvider.lastLocation
      : {
          coordinates: route.geometry[0],
          horizontalAccuracy: 0.0,
          courseOverGround: null,
          timestamp: Date.now(),
          speed: null,
        };

    const initialTripState = this.navigationController.getInitialState(startingLocation);
    this.tripState = initialTripState;

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

    const locationProvider = new BrowserLocationProvider();
    locationProvider.requestPermission();
    locationProvider.start();

    // TODO: This approach is not ideal, any better way to wait for the locationProvider to acquire the first location?
    while (!locationProvider.lastLocation) {
      await new Promise((resolve) => setTimeout(resolve, 100));
    }

    // Use the acquired user location to request the route
    const routes = await this.getRoutes(locationProvider.lastLocation, waypoints);
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
    this.locationProvider = locationProvider;
    this.startNavigation(route, config);
  }

  async stopNavigation() {
    // TODO: Factor out the UI layer from the core
    this.clearMap();
    this.routeAdapter?.free();
    this.routeAdapter = null;
    this.navigationController?.free();
    this.navigationController = null;
    this.tripState = null;
    if (this.locationProvider) this.locationProvider.updateCallback = null;
    if (this.useIntegratedSearchBox) this.map?.addControl(this.searchBox!, "bottom-left");
  }

  private onLocationUpdated() {
    this.tripState = this.navigationController!.updateUserLocation(this.locationProvider.lastLocation, this.tripState);
    this.currentLocationMapMarker?.setLngLat(this.locationProvider.lastLocation.coordinates);
    this.map?.easeTo({
      center: this.locationProvider.lastLocation.coordinates,
      bearing: this.locationProvider.lastLocation.courseOverGround.degrees || 0,
    });
  }

  private clearMap() {
    this.map?.getLayer("route") && this.map?.removeLayer("route");
    this.map?.getSource("route") && this.map?.removeSource("route");
    this.currentLocationMapMarker?.remove();
  }

  render() {
    return html`
      <div id="map">
        <instructions-view .tripState=${this.tripState}></instructions-view>
        <div id="bottom-component">
          <arrival-view .tripState=${this.tripState}></arrival-view>
          <button id="stop-button" @click=${this.stopNavigation} ?hidden=${!this.tripState}>
            <img src="/src/assets/directions/close.svg" alt="Stop navigation" class="icon" />
          </button>
        </div>
      </div>
    `;
  }
}
