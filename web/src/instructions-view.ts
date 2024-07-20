import { LitElement, css, html } from "lit";
import { customElement, property } from "lit/decorators.js";

@customElement("instructions-view")
export class InstructionsView extends LitElement {
  @property()
  tripState: any = null;

  static styles = [
    css`
      #view-card {
        padding: 10px;
        background-color: rgba(255, 255, 255, 0.9);
        border-radius: 8px;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
      }
    `,
  ];

  private roundToNearestTen(meters: number) {
    return Math.round(meters / 10) * 10;
  }

  render() {
    if (this.tripState?.Navigating) {
      return html`
        <div id="view-card">
          <p>${this.tripState.Navigating.visualInstruction.primaryContent.maneuverModifier}</p>
          <p>${this.tripState.Navigating.visualInstruction.primaryContent.maneuverType}</p>
          <p>${this.tripState.Navigating.visualInstruction.primaryContent.text}</p>
          <p>${this.roundToNearestTen(this.tripState.Navigating.progress.distance_to_next_maneuver)}m</p>
        </div>
      `;
    }
  }
}
