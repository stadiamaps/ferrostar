import { css, html, LitElement, PropertyValues } from "lit";
import { customElement, property } from "lit/decorators.js";
import maplibregl, { GeolocateControl, Marker } from "maplibre-gl";
import CloseSvg from "./assets/directions/close.svg";
import { TripState } from "@stadiamaps/ferrostar";
import "./instructions-view";
import "./trip-progress-view";
import { StateProvider as StateProvider } from "./types";

/**
 * A MapLibre-based map component.
 */
@customElement("ferrostar-map")
export class FerrostarMap extends LitElement {
  /**
   * The MapLibre map instance.
   *
   * You have to explicitly set this value when initializing
   * the web component to provide your own map instance.
   *
   */
  @property({ type: Object })
  map!: maplibregl.Map;

  @property({ type: Object })
  stateProvider: any;

  /**
   *  Styles to load which will apply inside the component
   */
  @property({ type: Object })
  customStyles?: object;

  @property()
  tripState: TripState | null = null;

  @property({ type: Object })
  route: any = null;

  /**
   * Determines whether the navigation user interface is displayed.
   */
  @property({ type: Boolean })
  showNavigationUI: boolean = false;

  /**
   * Should the user marker be shown on the map.
   * This is optional and defaults to false.
   */
  @property({ type: Boolean })
  showUserMarker: boolean = false;

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

  /**
   * A callback function that is invoked when navigation is stopped.
   * Optional: This function can be provided by the StateProvider.
   */
  @property({ type: Function, attribute: false })
  onStopNavigation?: () => void;

  /**
   * The geolocate control instance.
   */
  geolocateControl: GeolocateControl | null = null;

  private userLocationMarker: Marker | null = null;

  static styles = [
    css`
      [hidden] {
        display: none !important;
      }

      #container {
        height: 100%;
        width: 100%;
        position: relative;
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
  }

  firstUpdated() {
    if (this.addGeolocateControl && this.map) {
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

  updated(changedProperties: PropertyValues<this>) {
    /**
     * Render the route if changed properties have a route
     */
    if (changedProperties.has("route")) {
      this.renderRoute();
    }

    /**
     * Render the tripState if changed properties have a tripState
     */
    if (changedProperties.has("tripState") && this.tripState) {
      this.renderTripState();
    }
  }

  linkWith(stateProvider: StateProvider, showUserMarker: boolean = false) {
    // Check if already linked with an event provider
    if (this.stateProvider) {
      console.warn(
        "Already linked with an event provider. Please unlink first.",
      );
      return;
    }

    this.stateProvider = stateProvider;
    this.showUserMarker = showUserMarker;
    this.showNavigationUI = true;
    this.onStopNavigation = () => stateProvider.stopNavigation?.();

    // Listen to tripState updates
    this.stateProvider.addEventListener(
      "tripstate-update",
      (event: CustomEvent) => {
        this.tripState = event.detail.tripState;
      },
    );
  }

  /**
   * Renders the current tripState on the map.
   */
  private renderTripState() {
    if (!this.tripState || !this.map) {
      return;
    }

    this.updateCamera();
    if (this.showUserMarker) this.renderUserLocationMarker();
  }

  /**
   * Renders a route on the map.
   * If a route already exists, it clears the previous route before rendering a new one.
   * This method adds two layers to the map: the primary route layer and a border layer.
   */
  private renderRoute() {
    // Remove existing route if present
    this.clearRoute();

    if (!this.route?.geometry) return;

    this.map.addSource("route", {
      type: "geojson",
      data: {
        type: "Feature",
        properties: {},
        geometry: {
          type: "LineString",
          coordinates: this.route.geometry.map(
            (point: { lat: number; lng: number }) => [point.lng, point.lat],
          ),
        },
      },
    });

    // TODO: Configuration param where to insert the layer
    this.map.addLayer({
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

    this.map.addLayer(
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
  }

  /**
   * Renders or updates the user's location marker on the map. If a marker already exists, its position is updated.
   * Otherwise, a new marker is created and added to the map.
   */
  private renderUserLocationMarker() {
    if (!this.map || !this.tripState || !("Navigating" in this.tripState)) {
      return;
    }

    const userLocation = this.tripState.Navigating.userLocation;

    if (this.userLocationMarker) {
      this.userLocationMarker.setLngLat([
        userLocation.coordinates.lng,
        userLocation.coordinates.lat,
      ]);
      return;
    }

    this.userLocationMarker = new Marker({
      color: "blue",
    })
      .setLngLat([userLocation.coordinates.lng, userLocation.coordinates.lat])
      .addTo(this.map);
  }

  /**
   * Updates the camera position and orientation on the map based on the navState.
   * Adjusts the center of the map to the user's current location and sets the bearing
   * based on the user's course over ground, if available.
   */
  private updateCamera() {
    const tripState = this.tripState;
    if (!tripState || !("Navigating" in tripState)) return;

    this.map.easeTo({
      center: [
        tripState.Navigating.userLocation.coordinates.lng,
        tripState.Navigating.userLocation.coordinates.lat,
      ],
      bearing: tripState.Navigating.userLocation.courseOverGround?.degrees || 0,
    });
  }

  /**
   * Clears the route layers and source from the map if they exist.
   */
  private clearRoute() {
    if (this.map.getLayer("route")) this.map.removeLayer("route");
    if (this.map.getLayer("route-border")) this.map.removeLayer("route-border");
    if (this.map.getSource("route")) this.map.removeSource("route");
  }

  /**
   * Removes the user's location marker from the map if it exists and clears the reference.
   */
  private clearUserLocationMarker() {
    if (this.userLocationMarker) {
      this.userLocationMarker.remove();
      this.userLocationMarker = null;
    }
  }

  /**
   * Clears the current navigation state, including route, user location marker, and navigation-related data.
   */
  clearNavigation() {
    this.clearRoute();
    this.clearUserLocationMarker();
    this.route = null;
    this.tripState = null;
  }

  /**
   * Handles the logic for stopping the navigation process. It clears the navigation state,
   * resets the simulation flag, and invokes the optional onStopNavigation callback if provided.
   */
  private handleStopNavigation() {
    this.clearNavigation();
    this.showUserMarker = false;
    this.showNavigationUI = false;
    this.stateProvider = null;
    if (this.onStopNavigation) {
      this.onStopNavigation();
    }
  }

  render() {
    return html`
      <style>
        ${this.customStyles}
      </style>
      <div id="container">
        <div id="map">
          <slot></slot>
        </div>

        ${this.showNavigationUI
          ? html`
              <instructions-view
                .tripState=${this.tripState}
              ></instructions-view>
              <div id="bottom-component">
                <trip-progress-view
                  .tripState=${this.tripState}
                ></trip-progress-view>
                <button
                  id="stop-button"
                  @click=${this.handleStopNavigation}
                  ?hidden=${!this.tripState}
                >
                  <img src=${CloseSvg} alt="Stop navigation" class="icon" />
                </button>
              </div>
            `
          : ""}
      </div>
    `;
  }
}
