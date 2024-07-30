import { LitElement, PropertyValues, css, html } from "lit";
import { customElement, property } from "lit/decorators.js";

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
    const filename = `${this.getManeuverType().replaceAll(" ", "_")}_${this.getManeuverModifier().replaceAll(" ", "_")}.svg`;
    return new URL(`./assets/directions/${filename}`, import.meta.url).href;
  }

  render() {
    return html`<img src="${this.getImageUrl()}" alt="${this.getManeuverType()} ${this.getManeuverModifier()} maneuver" />`;
  }
}
