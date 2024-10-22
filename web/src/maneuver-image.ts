import { LitElement, css, html } from "lit";
import { customElement, property } from "lit/decorators.js";

const images = import.meta.glob("./assets/directions/*.svg", { eager: true });

@customElement("maneuver-image")
export class ManeuverImage extends LitElement {
  @property()
  visualInstruction: any = null;

  static styles = [
    css`
      img {
        width: 100%;
        height: auto;
      }
    `,
  ];

  getManeuverType() {
    return this.visualInstruction.primaryContent.maneuverType;
  }

  getManeuverModifier() {
    return this.visualInstruction.primaryContent.maneuverModifier;
  }

  getImageUrl() {
    // @ts-ignore
    return images[
      `./assets/directions/${this.getManeuverType().replaceAll(" ", "_")}_${this.getManeuverModifier().replaceAll(" ", "_")}.svg`
    ].default;
  }

  render() {
    return html`<img
      src="${this.getImageUrl()}"
      alt="${this.getManeuverType()} ${this.getManeuverModifier()} maneuver"
    />`;
  }
}
