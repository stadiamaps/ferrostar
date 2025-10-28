# Cutting a release

When cutting a release, follow this checklist:

1. Run `./update-release-version.sh X.Y.Z` with the new version string. Note that this is currently macOS-specific given the vagaries of GNU sed.
2. Commit staged changes and push.
3. Create a GitHub release and use the new version as the tag name (note that it must be in X.Y.Z format to please SPM).
4. Sit back and watch. GitHub actions take care of the rest.
   Note that iOS CI ends up adding a commit due to the way binary checksumming works.

## Release notes format

Each release must be accompanied by release notes.
The GitHub release notes are a good start, so we begin here.
However, this is really just a list of PRs,
and not all of them necessarily have a good description.

1. Generate GitHub release notes.
2. Rewrite descriptions for any PRs that are not particularly self-describing.
   Add a tag in front of each PR to indicate which layers it affects (Core, iOS, Android, Web, React Native).
3. Categorize the release notes into sections: Enhancements, Bug fixes, and Dependency Upgrades.
4. Add an `Upgrade Guide` section to the start of the release notes.

Here's a full example:

```markdown
# Upgrade guide

## Breaking changes

### [Core] Step Advance

This release introduces a number of changes to the step advance system. This may require some changes to your code, depending on which APIs you used to configure step advance.

* The `StepAdvanceCondition` trait definition has changed. If you implemented a condition on your own, you'll need to add the route deviation parameter to your `should_advance_step` method.
* `DistanceFromStepCondition` has a new required parameter that determines whether to advance when the user has deviated from the route.

### Other breaking changes

* [iOS] `FerrostarWidgetProvider`'s `update` method now takes an additional parameter for the current spoken instruction.

## Non-breaking highlights

* `WaypointAdvanceMode` has a new enum variant and improved documentation.
* `DistanceEntryAndSnappedExit` is a new built-in step advance condition. See https://github.com/stadiamaps/ferrostar/pull/722 for details and discussion.
* `Waypoint` can now accept arbitrary `properties`. This enables you to leverage all properties supported by the routing engine. Valhalla-based routers (incl. Stadia Maps) provide a _ton_ of flexibility, and you can access this in a type-safe manner! The Valhalla route provider documentation has more details and some example code: https://stadiamaps.github.io/ferrostar/route-providers.html#valhalla.
* We added a new step advance condition: `DistanceEntryAndSnappedExitCondition`. See the discussion on https://github.com/stadiamaps/ferrostar/pull/722 for details.

# PRs merged

## Enhancements

* [Core] Add route deviation to step advance conditions for short circuit tooling by @Archdoog in https://github.com/stadiamaps/ferrostar/pull/714
* [iOS] Improvements to Dynamic Island in the iOS demo app by @Archdoog in https://github.com/stadiamaps/ferrostar/pull/711
* [Core] Separate waypoint advance and step advance behaviors from each other by @Archdoog in https://github.com/stadiamaps/ferrostar/pull/716
* [Core] "Speed run" step advance until we can't advance anymore by @ianthetechie in https://github.com/stadiamaps/ferrostar/pull/715
* [React Native] React Native Expo example by @bjtrounson in https://github.com/stadiamaps/ferrostar/pull/718
* [Core] Add `DistanceEntryAndSnappedExitCondition` by @devnull133 in https://github.com/stadiamaps/ferrostar/pull/722
* [Core] Add arbitrary extended waypoint properties by @ianthetechie in https://github.com/stadiamaps/ferrostar/pull/721

## Bug fixes

* [iOS, Android] Fix deviation before arrive on demo app by @Archdoog in https://github.com/stadiamaps/ferrostar/pull/712

## Dependency Upgrades

* [Android] Bump androidx.test.espresso:espresso-core from 3.6.1 to 3.7.0 in /android by @dependabot[bot] in https://github.com/stadiamaps/ferrostar/pull/702
* [Android] Bump androidx-lifecycle from 2.9.2 to 2.9.4 in /android by @dependabot[bot] in https://github.com/stadiamaps/ferrostar/pull/703
* [Core] Bump serde_json from 1.0.142 to 1.0.145 in /common by @dependabot[bot] in https://github.com/stadiamaps/ferrostar/pull/704
* [Core] Bump uuid from 1.17.0 to 1.18.1 in /common by @dependabot[bot] in https://github.com/stadiamaps/ferrostar/pull/706
* [iOS] Bump github.com/pointfreeco/swift-snapshot-testing from 1.18.6 to 1.18.7 by @dependabot[bot] in https://github.com/stadiamaps/ferrostar/pull/708
* [Core] Bump proptest from 1.7.0 to 1.8.0 in /common by @dependabot[bot] in https://github.com/stadiamaps/ferrostar/pull/707
* [Core] Bump log from 0.4.27 to 0.4.28 in /common by @dependabot[bot] in https://github.com/stadiamaps/ferrostar/pull/709

## New Contributors

* @devnull133 made their first contribution in https://github.com/stadiamaps/ferrostar/pull/722

**Full Changelog**: https://github.com/stadiamaps/ferrostar/compare/0.43.0...0.44.0
```

## GPG

Maven Central requires all packages to be signed.
This necessarily means the headache of key management.
Fortunately keys don't need to be regenerated very often,
but here are some notes for whenever it's required again.

Sonatype has good [docs](https://central.sonatype.org/publish/requirements/gpg/) on generating a key.
To export the private key for use in CI,
you can run the following command.

```shell
gpg --armor --export-secret-key you@example.com | grep -v '\-\-' | grep -v '^=.' | tr -d '\n'
```
