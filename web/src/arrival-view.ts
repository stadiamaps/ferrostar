import { LitElement, html } from "lit";
import { customElement, property } from "lit/decorators.js";

@customElement("arrival-view")
export class ArrivalView extends LitElement {
  @property()
  tripState: any = null;

  firstUpdated() {
    console.log("ArrivalView firstUpdated");
    console.log(this.tripState?.Navigating.visualInstruction);
  }

  // updated(changedProperties: any) {
  //   if (changedProperties.has("visualInstruction") && this.visualInstruction) {
  //     console.log(this.visualInstruction);
  //   }
  // }

  getArrivalTime(seconds: number) {
    const now = new Date();
    const minutesToAdd = Math.round(seconds / 60);
    const arrivalTime = new Date(now.getTime() + minutesToAdd * 60000);
    const hours = arrivalTime.getHours();
    const minutes = arrivalTime.getMinutes();
    return `${hours}:${minutes < 10 ? '0' : ''}${minutes}`;
  }

  getDistanceRemaining(meters: number) {
    return `${Math.round(meters).toLocaleString()}m`;
  }

  getDurationRemaining(seconds: number) {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = Math.floor(seconds % 60);
    return `${minutes}m ${remainingSeconds}s`;
  }

  render() {
    if (this.tripState?.Navigating) {
      return html`
        <div>
          <p>${this.getArrivalTime(this.tripState.Navigating.duration_remaining)}</p>
          <p>${this.getDistanceRemaining(this.tripState.Navigating.progress.distance_remaining)}</p>
          <p>${this.getDurationRemaining(this.tripState.Navigating.progress.duration_remaining)}</p>
        </div>
      `;
    }
  }
}