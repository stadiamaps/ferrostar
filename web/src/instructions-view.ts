import { LitElement, css, html } from "lit";
import { customElement, property } from "lit/decorators.js";
import "./maneuver-image";

@customElement("instructions-view")
export class InstructionsView extends LitElement {
  @property()
  tripState: any = null;

  static styles = [
    css`
      .instructions-view-card {
        display: flex;
        align-items: center;
        padding: 20px;
        background-color: white;
        border-radius: 10px;
        box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
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

  private roundToNearestTen(meters: number) {
    return Math.round(meters / 10) * 10;
  }

  render() {
    // Note - lane information is currently not displayed, even if it is
    // available.
    if (this.tripState?.Navigating) {
      return html`
        <div class="instructions-view-card">
          <maneuver-image
            .visualInstruction=${this.tripState.Navigating.visualInstruction}
          ></maneuver-image>
          <div class="text-container">
            <p class="distance-text">
              ${this.tripState.Navigating.visualInstruction.primaryContent.text}
            </p>
            <p class="instruction-text">
              ${this.roundToNearestTen(
                this.tripState.Navigating.progress.distanceToNextManeuver,
              )}m
            </p>
          </div>
        </div>
      `;
    }
  }
}
