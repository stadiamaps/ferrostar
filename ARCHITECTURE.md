# Architecture Overview

NOTE: This is intentionally hand-wavy in some parts, and will be formalized more as design and development progress.

## Repo Structure

To ensure that everything can be developed properly in parallel,
we use a monorepo structure.
This, combined with CI, will ensure that changes in the core must be immediately reflected in frontends
like Apple and Android..

## Core (`common`)

Common logic is shared across platforms via the common core.
We chose to write this in Rust as it lacks most of the headaches
associated with C++, has interoperability with just about every platform. Recently, there have been huge strides in
idiomatic binding generation for Swift and Kotlin (see [UniFFI](https://github.com/mozilla/uniffi-rs) from Mozilla),
and Rust has some of the most loved build tooling of any programming language.

At the moment, we are attempting to keep the core purely functional.
This decision is not completely set in stone yet,
but it seems like an extremely logical choice in terms of testability.

### Navigation Controller

This manages the lifecycle of the navigation (TODO: define this more precisely).
When it receives an update on the user's current location,
it decides whether the user is still on course,
initiates recalculation if necessary and desired,
and pushes updates to the Navigation UI.
The navigation controller exposes information to the outside via a state model
that can be used to update the frontend UI (platform-specific).

### Routing Adapters

Routing adapters are responsible for facilitating route computation given two or more sequential waypoints.

Nothing in this crate is actually _generating_ routes via graph algorithms. It simply exists to facilitate the
request/response flow with a router. Most commonly this is done over HTTP, but with the actual interaction handled
by the platform-specific layer, not this crate. These are adapters in the "ports and adapters" sense.

## Platform-specific code

### Platform core

A platform-native wrapper is essential to keep the dev experience sane.
This is the public interface to the common framework
from the perspective of app code.

**Key responsibilities**

- Wrap core constructs and in some cases own them (navigation controller for example)
- Make network requests
- Ensure that operations are executed in the right context
  and that long-running ops are returned to user in accordance with platform norms
  (ex: async functions, callbacks, etc.)
- Interface with the platform-native location services
  and pass updates back to the core navigation controller
- Provide simulated/mocked location and network for testing where appropriate
- Enable extensibility (ex: local route generation, deciding whether to recalculate, etc.)

### UI

Ferrostar seeks to provide a default navigation UI for major platforms
at the discretion of the core team.
Default navigation UI implementations seek to provide a sensible experience
which could be used for the majority of common routing use cases.

We assume that most devs will want *some* flavor of this,
but that it should be customizable within reason.
As such, we do our best to make the implementation modular
(ex: via SwiftUI and JetPack Compose reusable components).
This way, devs may compose default implementations with their own.

Current platforms for which the core team maintains a UI:

* iOS (SwiftUI + MapLibre)
* Android (Jetpack Compose + MapLibre)

Note that while the currently bundled UI libraries utilize MapLibre,
there is no strong coupling.
You may add your own UI utilizing basemaps from any other vendor.
