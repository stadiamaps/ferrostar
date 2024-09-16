# Ferrostar

[Ferrostar](https://github.com/stadiamaps/ferrostar) is a modern SDK
for building turn-by-turn navigation applications.
It’s designed for customizability from the ground up,
helping you build the navigation experience your users deserve.

## Ferrostar is...

* **Modern** - The core is written in Rust, making it easy to contribute to, maintain, and port to new architectures.
  Platform specific libraries for iOS and Android leverage the best features of Swift and Kotlin.
* **Batteries included** - The bundled navigation UI is usable out of the box
  for the most common use cases, with minimal reconfiguration needed.
  Don't like our UI?
  Most components are reusable and composable
  thanks to SwiftUI, Jetpack Compose, and Web Components.
* **Extensible** - At every layer, you have flexibility to extend or replace functionality without needing to wait for a patch.
  Want to bring your own offline routing?
  Can do.
  Want to use your own detection logic to see if the user is off the route?
  Not a problem.
  Taken together with the batteries included approach,
  Ferrostar's goal is to **make simple things simple, and complex things possible**.
* **Vendor-neutral** - As a corollary to its extensibility, Ferrostar is vendor-neutral,
  and welcomes PRs to add support for additional [vendors](./vendors.md).
	The core Ferrostar components do not upload telemetry to any vendor
	(though developers may add their own).
* **Open-source** - Ferrostar is open-source. No funky strings; just BSD.

## Ferrostar is not...

- Aiming for compatibility with ancient SDKs / API levels, except where it’s easy; this is a rare chance for a fresh start.
- A routing engine, basemap, or search solution;
  there are many good [vendors](./vendors.md) that provide hosted APIs
  and offline route generation,
  and there is a rich ecosystem of FOSS software if you're looking to host your own for a smaller deployment.

## Can I use Ferrostar today?

### On iOS and Android

Ferrostar is is currently in **beta** for iOS and Android,
which means that it’s good to go for most use cases!
There will be a few rough edges and missing features,
but we’re here to help (check out the community links below).

The core is fully functional (pun intended for you FP lovers)
and ready to handle most use cases that we’re aware of!
If you’re already rolling a custom UI, you’re good to go!

iOS and Android have "batteries included" UIs
which are highly composable in nature thanks to SwiftUI and Jetpack Compose.
So you can customize most aspects of the UI today.

We know of at least half a dozen native app integrations underway,
and the core devs are dogfooding in their own apps.

### Using multiplatform frameworks

Ferrostar can be integrated into multiplatform frameworks
in a few ways.

Both Flutter and React Native have mechanisms for calling platform/native code,
which you can use to create and interact with
the `FerrostarCore` Swift and Kotlin classes.
Note that React Native is significantly more challenging as it is
deeply dependent on CocoaPods and has not yet adopted SPM (Ferrostar does).
If you are building a custom UI (ex: with [flutter_maplibre_gl](https://github.com/maplibre/flutter-maplibre-gl)
or [MapLibre React Native](https://github.com/maplibre/maplibre-react-native)),
then this is all you need.

For the UI, both Flutter and React Native include functionality for hosting native views,
and some community members are doing this successfully with Flutter already!

- [Flutter iOS Platform Views](https://docs.flutter.dev/platform-integration/ios/platform-views)
- [Flutter Android Platform Views](https://docs.flutter.dev/platform-integration/android/platform-views)

More idiomatic integrations are planned,
and contributions are very much welcome.
We are tracking status via the following issues:

- [Dart binding generation](https://github.com/stadiamaps/ferrostar/issues/16)
- [Flutter frontend components](https://github.com/stadiamaps/ferrostar/issues/106)
- [React Native core wrapper](https://github.com/stadiamaps/ferrostar/issues/116)

### On the web

The web platform is the newest addition to the family of supported platforms.
It is currently **alpha** quality.
We expect it to have the first beta release this autumn.

## How to use this guide
  
This guide is broken up into several sections.
The tutorial is designed to get you started quickly.
Read this first (at least the chapter for your platform).
Then you can pretty much skip around at will.

If you want to go deeper and customize the user experience,
check out the chapters on customization.

The architecture section documents the design of Ferrostar and its various components.
If you want to add support for a new routing API, post-process location updates,
or contribute to the development of Ferrostar, this is where the authoritative docs live.
(If you want to contribute, be sure to check out [CONTRIBUTING.md](https://github.com/stadiamaps/ferrostar/blob/main/CONTRIBUTING.md)!)

## Connect with the Community

Feel free to [open an issue or discussion on GitHub](https://github.com/stadiamaps/ferrostar/)
for bug reports, feature requests, and questions.
You can also join the `#ferrostar` channel on the [OSM US Slack](https://slack.openstreetmap.us/) for updates + discussion.
The core devs are active there and happy to answer questions / help you get started!