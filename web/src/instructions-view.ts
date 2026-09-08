import { LitElement, css, html } from "lit";
import { customElement, property } from "lit/decorators.js";
import { DistanceSystem } from "@maptimy/platform-formatters";
import "./maneuver-image";
import { formatDistance } from "./formatting";
import { TripState } from "@stadiamaps/ferrostar";

@customElement("instructions-view")
export class InstructionsView extends LitElement {
  @property()
  tripState: TripState | null = null;

  @property()
  system: DistanceSystem = "metric";

  @property()
  maxDecimalPlaces = 2;

  static styles = [
    css`
      .instructions-view-card {
        display: flex;
        align-items: center;
        padding: 20px;
        background-color: white;
        border-radius: 10px;
        box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
        font-family: sans-serif;
      }

      maneuver-image {
        flex: 1;
        max-width: 100px;
        min-width: 50px;
        height: auto;
        margin-right: 20px;
      }

      .text-container {
        display: flex;
        flex-direction: column;
        font-size: x-large;
      }

      .distance-text {
        color: black;
        font-weight: bold;
        margin: 0;
      }

      .instruction-text {
        color: #424242;
        margin: 10px 0 0 0;
      }
    `,
  ];

  render() {
    const ts = this.tripState;
    if (ts && "Navigating" in ts) {
      const nav = ts.Navigating;
      return html`
        <div class="instructions-view-card">
          <maneuver-image
            .visualInstruction=${nav.visualInstruction}
          ></maneuver-image>
          <div class="text-container">
            <p class="distance-text">
              ${nav.visualInstruction?.primaryContent.text}
            </p>
            <p class="instruction-text">
              ${formatDistance(
                nav.progress.distanceToNextManeuver,
                this.system,
                this.maxDecimalPlaces,
              )}
            </p>
          </div>
        </div>
      `;
    }
  }
}
