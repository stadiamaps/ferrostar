import type {
  Route,
  RouteAdapterInterface,
  Waypoint,
  UserLocation,
} from "ferrostar-rn-uniffi";
import { RouteAdapter, RouteRequest } from "ferrostar-rn-uniffi";
import { ab2json } from "./_utils";
import {
  InvalidStatusCodeException,
  NoRequestBodyException,
  NoResponseBodyException,
} from "./FerrostarCoreException";

export type RouteProviderInterface = {
  getRoute(
    userLocation: UserLocation,
    waypoints: Array<Waypoint>,
  ): Promise<Array<Route>>;
};

export class RouteProvider implements RouteProviderInterface {
  adapter: RouteAdapterInterface;

  constructor(
    valhallaEndpointURL: string,
    profile: string,
    options: Record<string, any> = {},
  ) {
    this.adapter = RouteAdapter.newValhallaHttp(
      valhallaEndpointURL,
      profile,
      JSON.stringify(options),
    );
  }

  async getRoute(
    userLocation: UserLocation,
    waypoints: Array<Waypoint>,
  ): Promise<Array<Route>> {
    const request = this.adapter.generateRequest(userLocation, waypoints);
    if (
      !RouteRequest.HttpPost.instanceOf(request) ||
      request.inner.body.byteLength <= 0
    ) {
      throw new NoRequestBodyException();
    }

    const { inner } = request;
    const body = ab2json(inner.body);

    const response = await fetch(inner.url, {
      method: "POST",
      ...inner.headers,
      body: JSON.stringify(body),
    });

    const bytes = await response.arrayBuffer();
    if (!response.ok) {
      throw new InvalidStatusCodeException(response.status);
    } else if (bytes.byteLength === 0) {
      throw new NoResponseBodyException();
    }

    return this.adapter.parseResponse(bytes);
  }
}
