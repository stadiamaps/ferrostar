# Ferrostar

[Ferrostar](https://github.com/stadiamaps/ferrostar) is a modern SDK for building turn-by-turn navigation applications.

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
	The core Ferrostar components do not upload telemetry to any vendor
	(though developers may add their own).
* **Open-source** - Ferrostar is open-source. No funky strings; just BSD.

## Ferrostar is not...

- Aiming for compatibility with ancient SDKs / API levels, except where it’s easy; this is a rare chance for a fresh start.
- A routing engine; there are many good [vendors](./vendors.md) that provide hosted APIs and offline route generation, as well as a rich ecosystem of FOSS software if you're looking to host your own for a smaller deployment.
- Building UI components for addresses search (look at [vendor](./vendors.md) SDKs that can help with this) or complex trip planning.

## Terminology and conventions

In this guide, we will use the following terms as specified.
Cases where a more narrow interpretation is needed should be obvious.

* **Interface** - When used in a context that’s talking about code,
  we use this term to mean a method or type signature.
  For example, we will use the term interface to refer to Kotlin interfaces,
  Swift protocols, and Rust traits.
  We also use the term to refer to a type’s *public interface*
  as in the available properties to an end user such as yourself.
* **Kotlin** - We’ll be quite loose when talking about “Kotlin.”
  It would be too cumbersome to write out something like Kotlin/Java or
  “your favorite JVM language.”
  When we speak of Kotlin, we usually mean any JVM language,
  except when referring to specific Kotlin features.
  While all example code is in Kotlin,
  things should work equally well in Java.
* **Platform** - When we refer to “platform libraries”, the “platform layer”,
  similar, we are referring to code written for/targeting
  the end deployment platform.
  Not the Rust core but platform-specific code like Swift or Kotlin.
  
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

## Getting help

If you can’t find what you want,
feel free to [open an issue or discussion on GitHub](https://github.com/stadiamaps/ferrostar/).
You can also join the `#ferrostar` channel on the [OSM US Slack](https://slack.openstreetmap.us/) for updates + discussion.
The core devs are active there and we're happy to answer questions / help you get started!