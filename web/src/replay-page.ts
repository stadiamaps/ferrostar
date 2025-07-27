import { LitElement } from "lit";
import { property, state } from "lit/decorators.js";
import { ReplayController } from "./replay-controller";
import maplibregl from "maplibre-gl";
import { TripState } from "@stadiamaps/ferrostar";

export class ReplayPage extends LitElement {
  @state()
  protected _tripState: TripState | null = null;

  private marker: maplibregl.Marker | null = null;
  private replay: ReplayController;

  @property({ type: Object })
  private map!: maplibregl.Map;

  constructor(recording: string) {
    super();
    this.replay = new ReplayController(recording);
  }

  firstUpdated() {
    this.map?.addSource("route", {
      type: "geojson",
      data: {
        type: "Feature",
        properties: {},
        geometry: {
          type: "LineString",
          coordinates: this.replay.route.geometry.map(
            (point: { lat: number; lng: number }) => [point.lng, point.lat],
          ),
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
      center: this.replay.route.geometry[0],
    });

    this.marker = new maplibregl.Marker({
      color: "green",
    })
      .setLngLat(this.replay.route.geometry[0])
      .addTo(this.map!);
  }
}
