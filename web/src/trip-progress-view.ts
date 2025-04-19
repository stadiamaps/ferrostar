import {
  LocalizedDurationFormatter,
  LocalizedDistanceFormatter,
} from "@maptimy/platform-formatters";
import { LitElement, css, html } from "lit";
import { customElement, property } from "lit/decorators.js";

const DurationFormatter = LocalizedDurationFormatter();
const DistanceFormatter = LocalizedDistanceFormatter();

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
        font-family: sans-serif;
      }

      .arrival-text {
        font-size: medium;
        margin: 0 15px;
        white-space: nowrap;
      }
    `,
  ];

  getEstimatedArrival(durationRemaining: number) {
    return new Date(new Date().getTime() + durationRemaining * 1000);
  }

  render() {
    if (this.tripState?.Navigating) {
      return html`
        <div class="progress-view-card">
          <p class="arrival-text">
            ${this.getEstimatedArrival(
              this.tripState.Navigating.progress.durationRemaining,
            ).toLocaleTimeString([], {
              hour: "2-digit",
              minute: "2-digit",
            })}
          </p>
          <p class="arrival-text">
            ${DurationFormatter.format(
              this.tripState.Navigating.progress.durationRemaining,
            )}
          </p>
          <p class="arrival-text">
            ${DistanceFormatter.format(
              this.tripState.Navigating.progress.distanceRemaining,
            )}
          </p>
        </div>
      `;
    }
  }
}
