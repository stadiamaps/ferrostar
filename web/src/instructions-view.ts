import { LitElement, html } from "lit";
import { customElement, property } from "lit/decorators.js";

@customElement("instructions-view")
export class InstructionsView extends LitElement {
  @property()
  tripState: any = null;

  firstUpdated() {
    console.log("InstructionsView firstUpdated");
    console.log(this.tripState?.Navigating.visualInstruction);
  }

  // updated(changedProperties: any) {
  //   if (changedProperties.has("visualInstruction") && this.visualInstruction) {
  //     console.log(this.visualInstruction);
  //   }
  // }

  private roundToNearestTen(meters: number) {
    return Math.round(meters / 10) * 10;
  }

  render() {
    if (this.tripState?.Navigating) {
      return html`
        <div>
          <p>${this.tripState.Navigating.visualInstruction.primaryContent.maneuverModifier}</p>
          <p>${this.tripState.Navigating.visualInstruction.primaryContent.maneuverType}</p>
          <p>${this.tripState.Navigating.visualInstruction.primaryContent.text}</p>
          <p>${this.roundToNearestTen(this.tripState.Navigating.progress.distance_to_next_maneuver)}</p>
        </div>
      `;
    }
  }
}
