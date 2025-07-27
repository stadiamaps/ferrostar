import { css, html, LitElement, PropertyValues } from "lit";
import { customElement, property } from "lit/decorators.js";
import maplibregl, { GeolocateControl } from "maplibre-gl";
import CloseSvg from "./assets/directions/close.svg";
import { SerializableNavState } from "@stadiamaps/ferrostar";

/**
 * A MapLibre-based map component.
 */
@customElement("ferrostar-map")
export class FerrostarMap extends LitElement {
  /**
   * The MapLibre map instance.
   */
  @property({ type: Object })
  map!: maplibregl.Map;

  /**
   *  Styles to load which will apply inside the component
   */
  @property({ type: Object })
  customStyles?: object;

  @property()
  navState: SerializableNavState | null = null;

  @property({type: Object})
  route: any = null;

  @property({ type: Boolean })
  showNavigationUI: boolean = false;

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

  @property({ type: Function, attribute: false })
  onStopNavigation?: () => void;

  /**
   * The geolocate control instance.
   */
  geolocateControl: GeolocateControl | null = null;

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
    `,
  ];

  constructor() {
    super();
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

  updated(changedProperties: PropertyValues<this>) {
    if (changedProperties.has("route")) {
      this.renderRoute();
    }
    if (changedProperties.has("navState") && this.navState) {
      this.renderNavState();
    }
  }

  private renderNavState() {
    if (!this.navState || !this.map) {
      return;
    }

    this.updateCamera();
  }

  private renderRoute() {
    // Remove existing route if present
    this.clearRoute();

    if (!this.navState?.tripState || !("Navigating" in this.navState.tripState)) return;

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

  private updateCamera() {
    const tripState = this.navState?.tripState;
    if (!tripState || !("Navigating" in tripState)) return;

    this.map.easeTo({
      center: [
        tripState.Navigating.userLocation.coordinates.lng,
        tripState.Navigating.userLocation.coordinates.lat,
      ],
      bearing: tripState.Navigating.userLocation.courseOverGround?.degrees || 0,
    });
  }

  private clearRoute() {
    if (this.map.getLayer("route")) this.map.removeLayer("route");
    if (this.map.getLayer("route-border")) this.map.removeLayer("route-border");
    if (this.map.getSource("route")) this.map.removeSource("route");
  }

  private handleStopNavigation() {
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
                .tripState=${this.navState?.tripState}
              ></instructions-view>
              <div id="bottom-component">
                <trip-progress-view
                  .tripState=${this.navState?.tripState}
                ></trip-progress-view>
                <button
                  id="stop-button"
                  @click=${this.handleStopNavigation}
                  ?hidden=${!this.navState?.tripState}
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
