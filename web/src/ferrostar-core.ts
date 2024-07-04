import { LitElement, html, css, unsafeCSS } from "lit";
import { customElement, property } from "lit/decorators.js";
import leafletStyles from "leaflet/dist/leaflet.css?inline";
import L from "leaflet";
import init, { NavigationController, RouteAdapter } from "ferrostar";

@customElement("ferrostar-core")
class FerrostarCore extends LitElement {
  @property()
  valhallaEndpointUrl: string = "";

  @property()
  profile: string = "";

  @property({ attribute: false })
  httpClient?: Function = fetch;

  // FIXME: type
  @property()
  locationProvider!: any;

  routeAdapter: RouteAdapter | null = null;
  map: L.Map | null = null;
  navigationController: NavigationController | null = null;

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
  }

  firstUpdated() {
    this.map = L.map(this.shadowRoot!.getElementById("map")!).setView([0, 0], 13);

    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(this.map);
  }

  // FIXME: type
  async getRoutes(initialLocation: any, waypoints: any) {
    await init();
    this.routeAdapter = new RouteAdapter(this.valhallaEndpointUrl, this.profile);

    // FIXME: find a better way to get the timestamp?
    const timestamp = {
      secs_since_epoch: Math.round(new Date().getTime() / 1000),
      nanos_since_epoch: 0,
    };

    const userLocation = {
      coordinates: initialLocation,
      horizontal_accuracy: 6.0,
      course_over_ground: null,
      timestamp: timestamp,
      speed: null,
    };

    const body = this.routeAdapter.generate_request(userLocation, waypoints).get("body");
    // FIXME: assert httpClient is not null
    const response = await this.httpClient!(this.valhallaEndpointUrl, {
      method: "POST",
      // FIXME: assert body is not null
      body: new Uint8Array(body).buffer,
    });
    const responseData = new Uint8Array(await response.arrayBuffer());
    const routes = this.routeAdapter.parse_response(responseData);

    return routes;
  }

  // FIXME: type
  async startNavigation(route: any, config: any) {
    this.navigationController = new NavigationController(route, config);

    const timestamp = {
      secs_since_epoch: Math.round(new Date().getTime() / 1000),
      nanos_since_epoch: 0,
    };

    const startingLocation = this.locationProvider.lastLocation
      ? this.locationProvider.lastLocation
      : {
          coordinates: route.geometry[0],
          horizontal_accuracy: 0.0,
          course_over_ground: null,
          timestamp: timestamp,
          speed: null,
        };

    // FIXME: should be camelCase
    const initialTripState = this.navigationController.get_initial_state(startingLocation);
    this.handleStateUpdate(initialTripState, startingLocation);

    // FIXME: since simulated location provider is not implemented yet, we are not moving in the map!
    const polyline = L.polyline(route.geometry, { color: "red" }).addTo(this.map!);
    this.map!.fitBounds(polyline.getBounds());
  }

  async replaceRoute(route: any, config: any) {
    // TODO
  }

  async advanceToNextStep() {
    // TODO
  }

  async stopNavigation() {
    // TODO
  }

  private async handleStateUpdate(newState: any, location: any) {
    // TODO
  }

  render() {
    return html`<div id="map"></div>`;
  }
}
