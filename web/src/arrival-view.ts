import { LitElement, css, html } from "lit";
import { customElement, property } from "lit/decorators.js";

@customElement("arrival-view")
export class ArrivalView extends LitElement {
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

  getArrivalTime(seconds: number) {
    const now = new Date();
    const minutesToAdd = Math.round(seconds / 60);
    const arrivalTime = new Date(now.getTime() + minutesToAdd * 60000);
    const hours = arrivalTime.getHours();
    const minutes = arrivalTime.getMinutes();
    return `${hours}:${minutes < 10 ? "0" : ""}${minutes}`;
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
        <div id="view-card">
          <p>${this.getArrivalTime(this.tripState.Navigating.progress.duration_remaining)}</p>
          <p>${this.getDistanceRemaining(this.tripState.Navigating.progress.distance_remaining)}</p>
          <p>${this.getDurationRemaining(this.tripState.Navigating.progress.duration_remaining)}</p>
        </div>
      `;
    }
  }
}
