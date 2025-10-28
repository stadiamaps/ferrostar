# Route Providers

Route providers expose common interfaces for making requests to a routing engine
and getting the data back in a standardized format for navigation.
This layer of indirection makes Ferrostar extremely extensible.

NOTE: Extensible route providers are currently available on iOS and Android.
The JavaScript platform presents some unique challenges,
so only Valhalla backends are supported directly from the JavaScript API
and published web components.
Contributions and discussion around the best ways to enable this are very much welcome.

## `RouteProvider`

There are two types of route providers:
one more suited to HTTP servers (`RouteAdapter`)
and another designed for other use cases like local route generation (`CustomRouteProvider`).
The core ships with common implementations,
but you’re free to define your own *in your application code*
without waiting for a PR to land or running a custom fork!

### `RouteAdapter`s

A route adapter is the first class of route provider.
It is designed for HTTP, sockets, and other request/response flows.
A [`RouteAdapter`](https://docs.rs/ferrostar/latest/ferrostar/routing_adapters/struct.RouteAdapter.html)
consists of two halves:
a [`RouteRequestGenerator`](https://docs.rs/ferrostar/latest/ferrostar/routing_adapters/trait.RouteRequestGenerator.html)
and a [`RouteResponseParser`](https://docs.rs/ferrostar/latest/ferrostar/routing_adapters/trait.RouteResponseParser.html).
At the Rust level, both of these are traits.
In platform code, you'll see a language-relevant version, such as a `protocol` in Swift
or an `interface` in Kotline.

Let’s illustrate why we chose the architecture of two separate halves with a real-world example.
[Valhalla](https://github.com/valhalla/valhalla)
can generate responses in multiple formats.
The default is its own JSON format,
but it also has more compact Protobuf and (much) more verbose OSRM serializers.

The OSRM-compatible serializer is the one that’s typically used for navigation use cases.
So, with a single `RouteResponseParser`, implementation,
we can parse responses from a Valhalla server or an OSRM server!
The Stadia Maps API is backed by Valhalla and returns responses in this format,
and both the Mapbox Directions API and GraphHopper (in certain modes),
as they also offer OSRM-compatible responses.

Every “extended OSRM” API includes additional data
which are useful for navigation applications.
**The parser in the core of Ferrostar handles all of these “flavors” gracefully,**
**and provides either direct or indirect support for most extensions.**
Of special note, the voice and banner instructions
(popularized by Mapbox and supported in Valhalla directly now)
are always parsed, when available, and included in the route object.
Annotations, which are available in some form or other on all OSRM-like APIs,
can be parsed as *anything*.
This leaves annotations open to extension,
since it’s already used this way in practice.
See the [Annotations documentation](./annotations.md) for more on those.

OSRM parser in hand, all that we need to do to support these different APIs
is a `RouteRequestGenerator` for each.
While all services mentioned are HTTP-based,
each has a different request format.
Writing a `RouteRequestGenerator` is pretty easy though.
Request generators return a sum type (enum/sealed class)
indicating the type of request to make
and associated data like the URL, headers, and request body.

By splitting up the request generation,
request execution, and response parsing into distinct steps,
we reduce the work required to support a new API.
In this example, all we had to do was supply a `RouteRequestGenerator`.
Our `RouteAdapter` was able to use the existing `OsrmResponseParser`,
and the core (ex: `FerrostarCore` on iOS or Android)
used the platform native HTTP stack to execute the request on our behalf.

Here’s a sequence diagram illustrating the flow of data between components.
Don’t worry too much about the internal complexity;
you’ll only interface with `FerrostarCore` at the boundary.

```mermaid
sequenceDiagram
    FerrostarCore->>+RouteAdapter: generateRequest
    RouteAdapter-->>+FerrostarCore: RouteRequest
    FerrostarCore-)Routing API: Network request
    Routing API--)FerrostarCore: Route response (bytes)
    FerrostarCore->>+RouteAdapter: parseResponse
    RouteAdapter->>+FerrostarCore: [Route] or error
    FerrostarCore->>+Application Code: [Route] or error
```

#### Bundled support

Ferrostar includes support for the following APIs out of the box.

##### Valhalla

Valhalla APIs (including hosted APIs that support the Valhalla API, like Stadia Maps)
are supported out of the box in Ferrostar.
As a bundled provider, `FerrostarCore` in the platform layer exposes
convenience initializers which automatically configure the `RouteAdapter`.

The Valhalla route provider supports rich locations via the `ValhallaWaypointProperties` struct.
This is exposed via a top-level helper function named `create_waypoint_with_valhalla_properties`
(camelCased in some languages).
Here's an example in Swift:

```swift
let waypoint = createWaypointWithValhallaProperties(coordinate: GeographicCoordinate(lat: 60.5349908, lng: -149.5485806),
                                                    kind: .break,
													properties: ValhallaWaypointProperties(preferredSide: .same))
```

If you’re rolling your own integration or curious about implementation details,
the relevant Rust type is [`ValhallaHttpRequestGenerator`](https://docs.rs/ferrostar/latest/ferrostar/routing_adapters/valhalla/struct.ValhallaHttpRequestGenerator.html).

As noted above, Valhalla supports an extended OSRM response format.
This is the only supported option at this time,
since the OSRM format has more detail than the "native" JSON format.

You can construct an instance of `ValhallaHttpRequestGenerator` directly
(if you’re using Rust for your application)
or using the convenience method `createValhallaRequestGenerator`
from Swift or Kotlin.


##### OSRM

OSRM has become something of a de facto *linga franca* for navigation APIs.
Ferrostar comes bundled with support for decoding OSRM responses,
including the common extensions developed by Mapbox and offered by many Valhalla servers.
This gives drop-in compatibility with a wide variety of backend APIs,
including the hosted options from Stadia Maps and Mapbox,
as well as many self-hosted servers.

The relevant Rust type is `OsrmResponseParser`.
The autogenerated FFI bindings expose a `createOsrmResponseParser` method
in case you want to roll your own `RouteAdapter` for an API
that uses a different request format but returns OSRM format responses.

The OSRM response parser also supports parsing rich waypoint properties
that are specified in the OSRM spec.
Note that this does not necessarily include every property that you included at request time!
Valhalla (and by extension Stadia Maps) support something like a dozen waypoint properties,
but these details are not echoed back by the server.

#### Implementing your own `RouteAdapter`

If you'd like to implement your own `RouteAdapter` to support a new HTTP API,
you can either do this in your application code or directly in the Rust core.

If you’d rather keep the logic to yourself (ex: for an internal API),
you can implement your own in Swift or Kotlin.
Just implement/conform to one or both of
`RouteRequestGenerator` and `RouteResponseParser`
in your Swift and Kotlin code.

You may only need to implement one half or the other.
For example, to integrate with a custom API that returns OSRM responses
but has a different request format, you only need a `RouteRequestGenerator`;
you can re-use the OSRM response parser.

If you’re working with a routing engine that's used more widely,
we welcome PRs to get these directly into Ferrostar!
Have a look at the existing Valhalla (request generator)
and OSRM (response parser) implementations for inspiration.

Refer to the core test code on GitHub for examples which mock both halves.
TODO: Examples here after 1.0.

### `CustomRouteProvider`

`CustomRouteProvider` is implemented as a protocol/interface
in the Swift and Android SDKs as part of the platform-level Ferrostar core.
Custom route providers are most commonly used for local route generation,
but they can be used for just about anything.
Rather than imposing a clean (but rigid) request+response model
with opinionated use cases like HTTP in mind,
the custom route provider is just a single-method interface
(SAM for you Java or Kotlin devs)
for getting routes asynchronously.

Here’s a sequence diagram illustrating the flow of data between components
when using a `CustomRouteProvider`.

```mermaid
sequenceDiagram
    FerrostarCore-)CustomRouteProvider: getRoutes
    CustomRouteProvider--)FerrostarCore: [Route] or error
```

When using a custom provider, it can be challenging to construct the
Ferrostar `Route` object from your provider's response. Ferrostar offers
multiple shortcuts for this including `createRouteFromOsrm`
and `createRouteFromOsrmRoute`. These methods allow you create a Ferrostar `Route`
directly from OSRM formatted json byte data.

#### Example (Kotlin)

```kt
class MyCustomRouteProvider(
  private val client: MyClient
): CustomRouteProvider {

  private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

  override suspend fun getRoutes(
    userLocation: UserLocation,
    waypoints: List<Waypoint>
  ): List<Route> {
    val myRequest = SomeRequestType(userLocation, waypoints)
    val myResponse = client.getRoute(myRequest)

    // Convert our response's waypoints into ferrostar waypoints.
    val outWaypoints = myResponse.waypoints.map {
      it.toFerrostarWaypoint()
    }

    val jsonAdapter = moshi.adapter(MyOsrmRoute::class.java)
    // Using the intermediate OSRM route JSON. We map our custom response
    // data into ferrostar waypionts.
    val routes = myResponse.routes {
      val osrmRoute = jsonAdapter.toJson(it.route).encodeToByteArray()
      createRouteFromOsrmRoute(osrmRoute, outWaypoints, MyCustomRouteProvider.POLYLINE_PRECISION)
    }
    return routes
  }

  companion object {
    const val POLYLINE_PRECISION: UInt = 6u
  }
}
```

For different formats, you can manually
convert sub-types by initializing a Ferrostar `Route` directly, or contribute your
own route provider to the core rust code. Sharing this functionality in the core
avoids having to reimplement this verbose functionality for each platform.

## Using a `RouteProvider`

The parts of Ferrostar concerned with routing are managed
by the `FerrostarCore` class in the platform layer.
After you create a `RouteProvider` and pass it off to `FerrostarCore`
(or use a convenience constructor which does it all for you!),
you’re done.
Developers do not need to interact with the `RouteProvider` after construction.
`FerrostarCore` provides a high-level interface for getting routes,
which is as close as you usually get to them.

```mermaid
sequenceDiagram
    Your Application-)FerrostarCore: getRoutes
    FerrostarCore-)RouteProvider: (Hidden complexity)
    RouteProvider--)FerrostarCore: [Route] or error
    FerrostarCore--)Your Application: [Route] or error
```

This part of the platform layer follows the
[Hollywood principle](https://en.wiktionary.org/wiki/Hollywood_principle).
This provides elegant ways of configuring rerouting, which we cover in
[Configuring the Navigation Controller](./configuring-the-navigation-controller.md).
