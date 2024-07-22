import { LitElement, css, html } from "lit";
import { customElement, property } from "lit/decorators.js";

@customElement("arrival-view")
export class ArrivalView extends LitElement {
  @property()
  tripState: any = null;

  static styles = [
    css`
      .arrival-view-card {
        display: flex;
        align-items: center;
        justify-content: space-around;
        padding: 20px;
        background-color: white;
        border-radius: 50px;
        box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
      }

      .arrival-text {
        font-size: x-large;
        margin: 0 15px;
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
      console.log(this.tripState);
      return html`
        <div class="arrival-view-card">
          <p class="arrival-text">${this.getArrivalTime(this.tripState.Navigating.progress.durationRemaining)}</p>
          <p class="arrival-text">${this.getDistanceRemaining(this.tripState.Navigating.progress.distanceRemaining)}</p>
          <p class="arrival-text">${this.getDurationRemaining(this.tripState.Navigating.progress.durationRemaining)}</p>
        </div>
      `;
    }
  }
}
