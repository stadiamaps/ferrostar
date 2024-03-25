# Ferrostar

Ferrostar is a modern SDK for building turn-by-turn navigation applications.

## Ferrostar is...

* **Modern** - The core is written in Rust, making it easy to contribute to, maintain, and port to new architectures.
  Platform specific libraries for iOS and Android leverage the best features of Swift and Kotlin.
* **Batteries included** - Navigation UI should be usable out of the box
  for the most common use cases in native iOS and Android apps
  with minimal reconfiguration needed.
  Don't like our UI?
  Most components are reusable and composable thanks to SwiftUI and Jetpack Compose.
* **Extensible** - At every layer, you have flexibility to extend or replace functionality without needing to wait for a patch.
  Want to bring your own offline routing?
  Can do.
  Want to use your own detection logic to see if the user is off the route?
  Not a problem.
  Taken together with the batteries included approach,
  Ferrostar's aim is to make simple things simple, and complex things possible.
* **Vendor-neutral** - As a corollary to its extensibility, Ferrostar is vendor-neutral,
  and welcomes PRs to add support for additional [vendors](./vendors.md).
  We do not collect telemetry for any vendor (though developers may of course add their own when needed).
* **Open-source** - Ferrostar is open-source. No funky strings; just BSD.

## Ferrostar is not...

- A set of UI components for searching for addresses or building a trip (look at [vendor](./vendors.md) SDKs that can help with this).
- Aiming for compatibility with ancient SDKs / API levels, except where it’s easy; this is a rare change for a fresh start.
- A router; there are many good [vendors](./vendors.md) that provide hosted APIs and offline route generation, as well as a rich ecosystem of FOSS software if you're looking to host your own for a smaller deployment.
- Optimized for a "free roam" experience without any specific route (though it *should* totally be possible to plug Ferrostar into such an experience!).
