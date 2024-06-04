# Ferrostar

Ferrostar is a FOSS navigation SDK built from the ground up for the future.

[Why a new SDK?](https://stadiamaps.notion.site/Next-Gen-Navigation-SDK-f16f987bfa5a455296b0671636033cdb)

## Current status

The project is under active development, but rapidly approaching production usefulness.
The core is functional (pun intended for you FP lovers)
and complete enough to handle most use cases.
One of the developers is actively using Ferrostar for cycling navigation on iOS.
Android is slightly less polished on the UI side but is otherwise feature parity.
See the issue tracker and guide for more details.

Here's a quick breakdown of support by platform.

|   | iOS | Android |
| - | --- | ------- |
| Core library building | ✅ | ✅ |
| High-level core bindings | ✅ | ✅ |
| Simulated location provider | ✅ | ✅ |
| Live location provider | ✅ | ✅ |
| Composable UI - banners | ✅ | ✅ |
| Composable UI - MapLibre integration | ✅ | 👨‍💻 |
| Voice guidance (platform-native TTS) | ✅ | ✅ |

While there are some rough edges, eager developers can start integrating.
Note that the API is currently NOT stable and there will still be some breaking changes,
but the release notes should include details.

Join the `#ferrostar` channel on the [OSM US Slack](https://slack.openstreetmap.us/) for updates + discussion.
The core devs are active there and we're happy to answer questions / help you get started!

![A screenshot of the current status](screenshot.png)

## Getting Started

### As a User

Check out the [guide](https://stadiamaps.github.io/ferrostar/)!

### As a Contributor

See our [CONTRIBUTING](CONTRIBUTING.md) guide
for info on expectations and dev environment setup.
