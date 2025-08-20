import {
  LocalizedDurationFormatter,
  DistanceSystem,
} from "@maptimy/platform-formatters";
import { LitElement, css, html } from "lit";
import { customElement, property } from "lit/decorators.js";
import { formatDistance } from "./util";

const DurationFormatter = LocalizedDurationFormatter();

@customElement("trip-progress-view")
export class TripProgressView extends LitElement {
  @property()
  tripState: any = null;

  @property()
  system: DistanceSystem = "metric";

  @property()
  maxDecimalPlaces = 2;

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
      @media (max-width: 600px) {
        .progress-view-card {
          padding: 10px;
        }

        .arrival-text {
          margin: 0 5px;
        }
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
            ${formatDistance(
              this.tripState.Navigating.progress.distanceRemaining,
              this.system,
              this.maxDecimalPlaces,
            )}
          </p>
        </div>
      `;
    }
  }
}
