# Architecture Overview

NOTE: This is intentionally hand-wavy in some parts, and will be formalized more as design and development progress.

## Common core

These components will be standardized and cross-platform. We chose to write this in Rust as it lacks most of the headaches
associated with C++, has interoperability with just about every platform. Recently, there have been huge strides in
idiomatic binding generation for Swift and Kotlin (see [UniFFI](https://github.com/mozilla/uniffi-rs) from Mozilla),
and Rust has some of the most loved build tooling of any programming language.

### Navigation Controller

This manages the lifecycle of the navigation (TODO: define this more precisely). When it receives an
update on the user's current location, it decides whether the user is still on course, initiates
recalculation if necessary and desired, and pushes updates to the Navigation UI. The navigation controller
exposes information to the outside via a state model that can be used to update the frontend UI (platform-specific).

### Routing Adapters

Routing adapters are responsible for facilitating route computation given two or more sequential waypoints.

Nothing in this crate is actually _generating_ routes via graph algorithms. It simply exists to facilitate the
request/response flow with a router. Most commonly this is done over HTTP, but with the actual interaction handled
by the platform-specific layer, not this crate. These are adapters in the "ports and adapters" sense.

## Platform-specific code

### Platform core

A platform-native wrapper will be essential. This will be the public interface to the common framework from the
perspective of app code. The platform core code will not be used directly by *most* users of the SDK; it is mostly
for developers and advanced users that want to build their own navigation UI or bring their own proprietary routing.

**Key responsibilities**

- Wrap core constructs and in some cases own them (navigation controller for example)
- Make network requests
- Ensure that operations are executed in the right context and that long-running ops are returned to user in a platform-native async manner
- Interface with the platform-native location services and pass updates back to the core navigation controller (also functionality to simulate!)
- Enable extensibility (ex: route generation using Swift/Kotlin code, deciding whether to recalculate, etc.)

### Navigation UI

A default navigation UI (backed by a MapLibre map) will be available which attempts to provide a complete experience
for the vast majority of use cases. It is assumed that most devs will want this.

In the usual case, this will create (and own) the core instance until its lifecycle is complete, and *most*
users won't need to interact with the core directly.

**Tunables**

- Map style (ex: URL or raw style to pass to MapLibre)?
- Hooks for customizing styling (with sensible defaults for OMT)
    - Route line layer - where to insert it in the style, and how to style it
    - Road name layer?
- TBD definitely more
