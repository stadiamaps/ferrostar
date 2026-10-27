# Navigation UI exercises

[Trailblaze](https://block.github.io/trailblaze/) exercises destination selection,
simulated navigation, a maneuver change, and stopping in each demo app.
Recordings and reports support human review; visual differences do not fail CI.
Build, device, and flow errors do.

## Run locally

Install Trailblaze and the usual platform build prerequisites, then build the exercise demo:

- **Android:** from `android/`, run `./gradlew :demo-app:installUiExercise` with an emulator running.
- **iOS:** build and install the demo in Debug with
  `SWIFT_ACTIVE_COMPILATION_CONDITIONS='DEBUG FERROSTAR_UI_EXERCISE'`.
  Use the normal local XCFramework setup and copy the API key template; no key is required.
- **Web:** from `web/`, run `npm ci && npm run build:site`.

From the repository root, start `just serve-ui-exercise` in a separate terminal.
Android also needs `adb reverse tcp:8765 tcp:8765`.
Run `just ui-exercise android`, `just ui-exercise ios`, or `just ui-exercise web`.
Open the resulting HTML report, or inspect the session with `trailblaze app`.

Routing uses an existing local OSRM fixture.
Basemap tiles, styles, sprites, and glyphs load from OpenFreeMap during the exercise.
The web demo's CDN assets also require network access.
Web replay enables `TRAILBLAZE_WEB_REPLAY_CAPTURE=tree+jpeg` so step screenshots remain
available when a video is too large to embed in the HTML report.

## CI and configurations

Each platform workflow uploads reports and session archives and links them from the PR.

Add device configurations to each workflow's `matrix.include` with unique names.
Viewport sizes and locales live in `navigation.trail.yaml`;
use Trailblaze recording classifiers for configuration-specific steps.
Run `just test-ui-exercise-support` to check the fixture server and comment publisher.
