import { LitElement, html, css, unsafeCSS } from "lit";
import { customElement, property } from "lit/decorators.js";
import leafletStyles from "leaflet/dist/leaflet.css?inline";
import L from "leaflet";
import markerIconUrl from "../node_modules/leaflet/dist/images/marker-icon.png";
import markerIconRetinaUrl from "../node_modules/leaflet/dist/images/marker-icon-2x.png";
import markerShadowUrl from "../node_modules/leaflet/dist/images/marker-shadow.png";
import init, { NavigationController, RouteAdapter } from "ferrostar";

@customElement("ferrostar-core")
export class FerrostarCore extends LitElement {
  @property()
  valhallaEndpointUrl: string = "";

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

  routeAdapter: RouteAdapter | null = null;
  map: L.Map | null = null;
  navigationController: NavigationController | null = null;
  currentLocationMapMarker: L.Marker | null = null;

  // TODO: type
  tripState: any = null;

  static styles = [
    unsafeCSS(leafletStyles),
    css`
      #map {
        height: 100%;
        width: 100%;
      }
    `,
  ];

  constructor() {
    super();

    // A workaround for avoiding "Illegal invocation"
    if (this.httpClient === fetch) {
      this.httpClient = this.httpClient.bind(window);
    }

    // A workaround for loading the marker icon images in Vite
    L.Icon.Default.prototype.options.iconUrl = markerIconUrl;
    L.Icon.Default.prototype.options.iconRetinaUrl = markerIconRetinaUrl;
    L.Icon.Default.prototype.options.shadowUrl = markerShadowUrl;
  }

  updated(changedProperties: any) {
    if (changedProperties.has("locationProvider") && this.locationProvider) {
      this.locationProvider.updateCallback = this.onLocationUpdated.bind(this);
    }
  }

  firstUpdated() {
    this.map = L.map(this.shadowRoot!.getElementById("map")!).setView([0, 0], 13);

    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(this.map);
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
    this.handleStateUpdate(initialTripState, startingLocation);

    this.resetMap();

    const polyline = L.polyline(route.geometry, { color: "red" }).addTo(this.map!);
    this.map!.fitBounds(polyline.getBounds());

    this.currentLocationMapMarker = L.marker(route.geometry[0]).addTo(this.map!);
  }

  async replaceRoute(route: any, config: any) {
    // TODO
  }

  async advanceToNextStep() {
    // TODO
  }

  async stopNavigation() {
    this.resetMap();
    this.routeAdapter = null;
    this.locationProvider.updateCallback = null;
    this.navigationController = null;
    this.currentLocationMapMarker = null;
  }

  private async handleStateUpdate(newState: any, location: any) {
    // TODO
  }

  private onLocationUpdated() {
    this.tripState = this.navigationController!.updateUserLocation(this.locationProvider.lastLocation, this.tripState);
    this.currentLocationMapMarker!.setLatLng(this.locationProvider.lastLocation.coordinates);
  }

  private resetMap() {
    this.map!.eachLayer((layer) => {
      if (layer instanceof L.Marker || layer instanceof L.Polyline) {
        this.map!.removeLayer(layer);
      }
    });
  }

  render() {
    return html`<div id="map"></div>`;
  }
}
