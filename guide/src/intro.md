# Ferrostar

Ferrostar is an SDK for building turn-by-turn navigation applications.

## Ferrostar is...

* **Modern** - The core is written in Rust, making it easy to contribute to, maintain, and port to new architectures.
  Platform specific libraries for iOS and Android leverage the best features of Swift and Kotlin.
* **Batteries included** - Navigation UI should be usable out of the box for the most common use cases
  in native iOS and Android apps with minimal reconfiguration needed.
  Don't like our UI? Most of the components are reusable and composable thanks to SwiftUI and Jetpack Compose.
* ***Extensible** - At every layer, you have flexibility to extend or replace functionality without needing to wait for a patch.
  Want to bring your own offline routing?
  Can do.
  Want to use your own detection logic to see if the user is off the route?
  Not a problem.
* **Vendor-neutral** - As a corollary to its extensibility, Ferrostar is vendor-neutral,
  and welcomes PRs to add suport for additional [vendors](./vendors.md).
  We do not collect telemetry for any vendor (though developers may of course add their own when needed).

## Non-goals

The following are non-goals of the project. These are left to developers.

- UI components for searching for addresses or building a trip (this is highly vendor-specific and often needs customization per-app).
- Compatibility with ancient SDKs / API levels, except where it’s easy; this is a fresh start.
- Route generation; there are many good [vendors](./vendors.md) as well as self-hosting / local generation options.
- Building a "free roam" experience without any specific route (though it *should* be possible to plug Ferrostar into such an experience).
