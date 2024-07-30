import { LitElement, html, css, unsafeCSS } from "lit";
import { customElement, property } from "lit/decorators.js";
import maplibregl from "maplibre-gl";
import maplibreglStyles from "maplibre-gl/dist/maplibre-gl.css?inline";
import init, { NavigationController, RouteAdapter } from "ferrostar";
import "./instructions-view";
import "./arrival-view";

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

  routeAdapter: RouteAdapter | null = null;
  map: maplibregl.Map | null = null;
  navigationController: NavigationController | null = null;
  currentLocationMapMarker: maplibregl.Marker | null = null;

  static styles = [
    unsafeCSS(maplibreglStyles),
    css`
      #map {
        height: 100%;
        width: 100%;
      }

      instructions-view,
      arrival-view {
        position: absolute;
        left: 50%;
        transform: translateX(-50%);
        max-width: 80%;
        z-index: 1000;
      }

      #top-component {
        top: 10px;
      }

      #bottom-component {
        bottom: 10px;
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
    this.locationProvider.updateCallback = this.onLocationUpdated.bind(this);
    this.navigationController = new NavigationController(route, config);

    const startingLocation = this.locationProvider.lastLocation
      ? this.locationProvider.lastLocation
      : {
          coordinates: route.geometry[0],
          horizontalAccuracy: 0.0,
          courseOverGround: null,
          // TODO: find a better way to create the timestamp?
          timestamp: {
            secs_since_epoch: Math.floor(Date.now() / 1000),
            nanos_since_epoch: 0,
          },
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

  async stopNavigation() {
    // TODO: Factor out the UI layer from the core
    this.clearMap();
    this.routeAdapter = null;
    this.locationProvider.updateCallback = null;
    this.navigationController = null;
    this.currentLocationMapMarker = null;
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
        <instructions-view .tripState=${this.tripState} id="top-component"></instructions-view>
        <arrival-view .tripState=${this.tripState} id="bottom-component"></arrival-view>
      </div>
    `;
  }
}
