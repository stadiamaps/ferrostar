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

  getFilename() {
    return `${this.getManeuverType().replaceAll(" ", "_")}_${this.getManeuverModifier().replaceAll(" ", "_")}.svg`;
  }

  render() {
    console.log(this.getFilename())
    return html`<img src="/src/assets/directions/${this.getFilename()}" alt="${this.getManeuverType()} ${this.getManeuverModifier()} maneuver" />`;
  }
}
