# Annotations

The routing APIs from Stadia Maps, Mapbox, and others
use this to include detailed information like speed limits,
expected travel speed, traffic, and more.
These are expressed via annotations, a de facto standard from OSRM.

`FerrostarCore` includes support for parsing arbitrary annotations
from the route response,
and will handle the most common ones from Valhalla-based servers
(like Stadia Maps and Mapbox) with just one line of code.

The implementation is completely generic,
so you can define your own model to include custom parameters.
PRs welcome for other public API annotation models.

## Swift

Here’s how to create a Valhalla extended OSRM annotation publisher in Swift:

```swift
let annotationPublisher = AnnotationPublisher<ValhallaExtendedOSRMAnnotation>.valhallaExtendedOSRM()
```

Pass this publisher via the optional `annotation:` parameter
of the `FerrostarCore` constructor.

## Kotlin

In Kotlin, you can create a Valhalla extended OSRM annotation publisher
by importing and invoking an extension method:

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
