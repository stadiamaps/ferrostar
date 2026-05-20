# Session Recording

Reproducing a navigation bug from a screenshot or a vague description is difficult.
Ferrostar can record an entire navigation session as a JSON document for later analysis.
This lets you replay what the user "saw" either on the web or in your own app
for debugging issues.
This is especially helpful for troubleshooting subtle issues around step advance
and deviation detection.

<div class="warning">

Recording captures every navigation event for the lifetime of the session.
That includes the full state after every single GPS tick (usually 1/second),
so the logs can be quite large.
You shouldn't ship a production app with this unconditionally enabled.
Also note that the logs contain personal location information,
and it is your responsibility to treat this information with care.

</div>

## Recording a session

The [`NavigationRecorder`](https://docs.rs/ferrostar/latest/ferrostar/navigation_session/recording/struct.NavigationRecorder.html)
is the main entrypoint.
We've linked the Rust docs here as that's the source implementation.
The platform bindings look similar in every language.

### Swift

iOS attaches a recorder via the [`FerrostarSessionBuilder`](https://swiftpackageindex.com/stadiamaps/ferrostar/main/documentation/ferrostarcore/ferrostarsessionbuilder).
The convenience `FerrostarCore` initializers accept an optional `configureSessionBuilder` closure
that lets you attach observers (like a recorder).
You can also pass a configured session builder directly to the designated initializer.

```swift
import FerrostarCore
import FerrostarCoreFFI

// Create the recorder for the route you're about to navigate.
let recorder = NavigationRecorder(route: route, config: config.ffiValue)

let core = FerrostarCore(
    routeAdapter: routeAdapter,
    locationProvider: locationProvider,
    navigationControllerConfig: config,
    networkSession: URLSession.shared,
    configureSessionBuilder: { $0.withRecorder(recorder) }
)

// ... start navigation as usual via core.startNavigation(route:) ...

// When you're done, pull the serialized recording.
// The result is a JSON string that you can write it to disk,
// upload to a secure storage bucket, email, etc.
let json = try recorder.getRecordingJson()
```

### Kotlin

On Android, `FerrostarCore` exposes its `sessionBuilder` as a public property,
so you can either configure the builder up front
or attach a recorder to an already-constructed core.

```kotlin
import com.stadiamaps.ferrostar.core.FerrostarCore
import uniffi.ferrostar.NavigationRecorder

// Construct the recorder once you have the route.
val recorder = NavigationRecorder(route, config)

// Attach it to the session builder before starting navigation.
core.sessionBuilder.withRecorder(recorder)

// ... start navigation as usual via core.startNavigation(route) ...

// When you're done, pull the serialized recording.
// The result is a JSON string that you can write it to disk,
// upload to a secure storage bucket, email, etc.
val json = recorder.getRecordingJson()
```

If you prefer to configure the builder up front,
construct a `FerrostarSessionBuilder` yourself,
call `withRecorder` on it,
and pass it as the `sessionBuilder` argument to `FerrostarCore`.

### TypeScript

The web component exposes recording as a single boolean attribute on `<ferrostar-core>`.
When the attribute is set,
`stopNavigation()` automatically downloads the recording as `recording.json`
through a browser download.

```html
<ferrostar-core
    id="core"
    valhalla-endpoint-url="https://api.example.com/route/v1"
    profile="auto"
    should-record
></ferrostar-core>
```

Or, equivalently, when assigning the property in JavaScript:

```typescript
const core = document.getElementById("core") as FerrostarCore;
core.shouldRecord = true;
```

### Rust

If you're doing a low-level integration with Rust,
construct a [`NavigationRecorder`](https://docs.rs/ferrostar/latest/ferrostar/navigation_session/recording/struct.NavigationRecorder.html)
and pass it to [`NavigationSession::new`](https://docs.rs/ferrostar/latest/ferrostar/navigation_session/struct.NavigationSession.html) as an observer:

```rust
use std::sync::Arc;
use ferrostar::navigation_controller::NavigationController;
use ferrostar::navigation_session::{NavigationSession, recording::NavigationRecorder};

let recorder = Arc::new(NavigationRecorder::new(route.clone(), config.clone()));
let session = NavigationSession::new(
    Arc::new(NavigationController::new(route, config)),
    vec![recorder.clone()],
);

// ... drive the session as usual ...

// When you're done, pull the serialized recording.
// The result is a JSON string that you can write it to disk,
// upload to a secure storage bucket, email, etc.
let json = recorder.get_recording_json()?;
```

## Replaying a recording

We hoset a web-based replay tool at
[stadiamaps.github.io/ferrostar/web-demo/tools/replay/](https://stadiamaps.github.io/ferrostar/web-demo/tools/replay/).
Drop a `recording.json` into the upload field
and use the playback controls to scrub, pause, and adjust speed.

The source code for this lives under [`web/tools/replay/`](https://github.com/stadiamaps/ferrostar/tree/main/web/tools/replay)
and serves as an example implementation.
You can also build your own replay functionality into your app.

