# Annotations

The routing APIs from Stadia Maps, Mapbox, and others
use this to include detailed information like speed limits,
expected travel speed, traffic, and more.
These are expressed via annotations, a de facto standard from OSRM.

`FerrostarCore` includes support for parsing arbitrary annotations
from the route response,
and will handle the most common ones from Valhalla-based servers
(like Stadia Maps and Mapbox) with just one line of code.

## OSRM-style annotation data structure

In the OSRM data model, annotations are a list
with each entry representing a line segment between consecutive
coordinates along the route geometry.
This allows for fine-grained details which may change through the course of a maneuver.

While OSRM’s annotations aren’t particularly interesting for most use cases,
many implementations use this for speed limit, traffic congestion, and similar info.

The implementation in Ferrostar is generic,
so you can define your own model to include custom parameters.
PRs welcome for other public API annotation models.

## Setting up an annotation publisher

Here’s how to set up an annotation publisher using the bundled one
(for Valhalla-based solutions like Stadia Maps and Mapbox).

### Swift

```swift
let annotationPublisher = AnnotationPublisher<ValhallaExtendedOSRMAnnotation>.valhallaExtendedOSRM()
```

Pass this publisher via the optional `annotation:` parameter
of the `FerrostarCore` constructor.
Now you can use the `annotation` property on your `FerrostarCore` instance
to get the annotation based on the user’s snapped location.

### Kotlin

```kotlin
import com.stadiamaps.ferrostar.core.annotation.valhalla.valhallaExtendedOSRMAnnotationPublisher

// Elsewhere in your file...
val annotationPublisher = valhallaExtendedOSRMAnnotationPublisher()
```

The most common way to use this is in your view model.
If you’re subclassing the `DefaultNavigationViewModel`,
just pass this as a constructor argument like so:

```kotlin
class DemoNavigationViewModel(
    // This is a simple example, but these would typically be dependency injected
    val ferrostarCore: FerrostarCore = AppModule.ferrostarCore,
    val locationProvider: LocationProvider = AppModule.locationProvider,
    annotationPublisher: AnnotationPublisher<*> = valhallaExtendedOSRMAnnotationPublisher()
) : DefaultNavigationViewModel(ferrostarCore, annotationPublisher), LocationUpdateListener {
    // ...
}
```

## Displaying speed limits in your app

The provided navigation views for iOS and Android
supports speed limits out of the box!
Both demo apps include examples of how to configure it.

### SwiftUI

For SwiftUI, you can configure speed limit display using the `navigationSpeedLimit`
view modifier on your navigation view:

```swift
DynamicallyOrientingNavigationView(
    // Other arguments...
)
.navigationSpeedLimit(
    // Configure speed limit signage based on user preference or location
    speedLimit: ferrostarCore.annotation?.speedLimit,
    speedLimitStyle: .mutcdStyle
)
```

### Jetpack Compose

With Jetpack Compose, you can configure speed limit display with the optional
`config` parameter:

```kotlin
DynamicallyOrientingNavigationView(
    // Other arguments...
    // Configure speed limit signage based on user preference or location
    config = VisualNavigationViewConfig.Default().withSpeedLimitStyle(SignageStyle.MUTCD)
)
```