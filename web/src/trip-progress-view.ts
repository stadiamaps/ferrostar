import { LitElement, css, html } from "lit";
import { customElement, property } from "lit/decorators.js";

function roundToNearest(value: number, unit: number): number {
  return Math.round(value / unit) * unit;
}

@customElement("trip-progress-view")
export class TripProgressView extends LitElement {
  @property()
  tripState: any = null;

  static styles = [
    css`
      .progress-view-card {
        display: flex;
        align-items: center;
        justify-content: space-around;
        padding: 20px;
        background-color: white;
        border-radius: 50px;
        box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
      }

      .arrival-text {
        font-size: medium;
        margin: 0 15px;
        white-space: nowrap;
      }
    `,
  ];

  getArrivalTime(seconds: number) {
    const now = new Date();
    const minutesToAdd = Math.round(seconds / 60);
    const arrivalTime = new Date(now.getTime() + minutesToAdd * 60 * 1000);
    const hours = arrivalTime.getHours();
    const minutes = arrivalTime.getMinutes();
    return `${hours}:${minutes < 10 ? "0" : ""}${minutes}`;
  }

  getDistanceRemaining(meters: number) {
    // TODO: Consider extracting this to Rust; Kotlin has the same logic
    if (meters > 1_000) {
      let value = meters / 1_000;

      if (value > 10_000) {
        value = roundToNearest(value, 1);
      } else {
        value = roundToNearest(value, 0.1);
      }

      return `${value.toLocaleString()}km`;
    } else {
      let value;
      if (meters > 100) {
        value = roundToNearest(meters, 100);
      } else if (meters > 10) {
        value = roundToNearest(meters, 10);
      } else {
        value = roundToNearest(meters, 5);
      }

      return `${value.toLocaleString()}m`;
    }
  }

  getDurationRemaining(seconds: number) {
    const minutes = Math.floor(seconds / 60);
    return `${minutes}m`;
  }

  render() {
    if (this.tripState?.Navigating) {
      return html`
        <div class="progress-view-card">
          <p class="arrival-text">
            ${this.getArrivalTime(
              this.tripState.Navigating.progress.durationRemaining,
            )}
          </p>
          <p class="arrival-text">
            ${this.getDurationRemaining(
              this.tripState.Navigating.progress.durationRemaining,
            )}
          </p>
          <p class="arrival-text">
            ${this.getDistanceRemaining(
              this.tripState.Navigating.progress.distanceRemaining,
            )}
          </p>
        </div>
      `;
    }
  }
}
