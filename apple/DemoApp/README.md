#  Ferrostar iOS Demo

This project is a minimal demonstration of how to use [Ferrostar](https://github.com/stadiamaps/ferrostar)
in an iOS application.

Eventually we would like to merge this into the main repository,
but hit some [issues with local Swift packages](https://forums.swift.org/t/issue-with-local-binarytarget-and-local-package/68726).

## Quickstart

1. Sign up for a [free Stadia Maps account](https://client.stadiamaps.com/signup/?utm_content=ferrostar_ios&utm_campaign=ferrostar_demos&utm_source=github)
2. Go through property setup and create an API key.
3. Update the `Info.plist` (either via project settings or opening directly) with your API key.
4. Run the app!

NOTES:

* If you are running in the Simulator, set your location to Apple
* If you running on a device, you probably need to change the test location to some other place nearby (we plan to replace this with a map)
* At the moment, this is purely for functional testing; no effort has yet gone into polish
