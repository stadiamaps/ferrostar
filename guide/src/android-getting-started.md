# Getting Started on Android

This section of the guide covers how to integrate Ferrostar into an Android app.
We'll cover the "batteries included" approach, but flag areas for customization and overrides along the way.

Note that while this section is WIP, we have a well-documented [demo app](https://github.com/stadiamaps/ferrostar/tree/main/android/demo-app).
The TODOs will get filled in eventually, but the demo app is a good reference for now.

## Minimum requirements

See the [platform support targets](./platform-support-targets.md) document
for details on supported Android versions.

## Add dependencies

Ferrostar releases are hosted on GitHub Packages.
You’ll need to authenticate first in order to access them.
GitHub has a [guide on setting this up](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#authenticating-to-github-packages).

### Maven repository setup

Once you’ve configured GitHub credentials as project properties or environment variables,
Add the repository to your build script.

If you are using `settings.gradle` for your dependency resolution management,
you’ll end up with something like along these lines:

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = 'https://maven.pkg.github.com/stadiamaps/ferrostar'
            credentials {
                username = settings.ext.find('gpr.user') ?: System.getenv('GITHUB_ACTOR')
                password = settings.ext.find('gpr.key') ?: System.getenv('GITHUB_TOKEN')
            }
        }
        
        // For the MapLibre compose integration
        maven {
            url = 'https://maven.pkg.github.com/Rallista/maplibre-compose-playground'
            credentials {
                username = settings.ext.find('gpr.user') ?: System.getenv('GITHUB_ACTOR')
                password = settings.ext.find('gpr.key') ?: System.getenv('GITHUB_TOKEN')
            }
        }

        google()
        mavenCentral()
    }
}
```

And if you’re doing this directly in `build.gradle`, things look slightly different:

```groovy
repositories {
    google()
    mavenCentral()

    maven {
        url = uri("https://maven.pkg.github.com/stadiamaps/ferrostar")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.token") ?: System.getenv("GITHUB_TOKEN")
        }
    }
    
    // For the MapLibre compose integration
    maven {
        url = uri("https://maven.pkg.github.com/Rallista/maplibre-compose-playground")
        credentials {
            username = settings.ext.find("gpr.user") ?: System.getenv("USERNAME")
            password = settings.ext.find("gpr.token") ?: System.getenv("TOKEN")
        }
    }
}
```

### Add dependencies

#### `build.gradle` with explicit version strings

If you’re using the classic `build.gradle`
with `implementation` strings using hard-coded versions,
here’s how to set things up.
Replace `X.Y.Z` with an [appropriate version](https://github.com/orgs/stadiamaps/packages?repo_name=ferrostar).

```groovy
dependencies {
    // Elided: androidx dependencies: ktx and compose standard deps

    // Ferrostar
    def ferrostarVersion = 'X.Y.Z'
    implementation "com.stadiamaps.ferrostar:core:${ferrostarVersion}"
    implementation "com.stadiamaps.ferrostar:maplibreui:${ferrostarVersion}"

    // okhttp3
    implementation platform("com.squareup.okhttp3:okhttp-bom:4.10.0")
    implementation 'com.squareup.okhttp3:okhttp'
}
```

#### `libs.versions.toml` “modern style”

If you’re using the newer `libs.versions.toml` approach,
add the versions like so:

```toml
[versions]
ferrostar = "X.Y.X"

[libraries]
ferrostar-core = { group = "com.stadiamaps.ferrostar", name = "core", version.ref = "ferrostar" }
ferrostar-maplibreui = { group = "com.stadiamaps.ferrostar", name = "maplibreui", version.ref = "ferrostar" }
```

Then reference it in your `build.gradle`:

```groovy
dependencies {
    // Elided: androidx dependencies: ktx and compose standard deps

    // Ferrostar
    implementation libs.ferrostar.core
    implementation libs.ferrostar.maplibreui

    // okhttp3
    implementation platform("com.squareup.okhttp3:okhttp-bom:4.10.0")
    implementation 'com.squareup.okhttp3:okhttp'
}
```

## Configure location services

### TODO: Permissions and privacy settings

The usual Android setup.

### Location providers

You'll need to configure a provider to get location updates.
We bundle a few implementations to get you started, or you can create your own.
The broad steps are the same regardless of which provider you use:
create an instance of the class,
store it in an instance variable where it makes sense,
and (if simulating a route) set the location manually or enter a simulated route.

Similar to the Android location APIs you may already know,
you can add or remove listeners which will receive updates.

#### `SimulatedLocationProvider`

The `SimulatedLocationProvider` allows for simulating location within Ferrostar
without needing GPX files or complicated environment setup.
This is great for testing and development without stepping outside.

First, instantiate the class.
This will typically be saved as an instance variable.

```kotlin
private val locationProvider = SimulatedLocationProvider()
```

Later, most likely somewhere in your activity creation code or similar,
set a location to your desired simulation start point.

```kotlin
locationProvider.lastLocation = initialSimulatedLocation
```

Once you have a route, you can simulate the replay of the route.
This is technically optional (you can just set `lastLocation` yourself too),
but playing back a route saves you the effort.
You can set a `warpFactor` to play it back faster.

```kotlin
locationProvider.warpFactor = 2u
locationProvider.setSimulatedRoute(route)
```

You don’t need to do anything else after setting a simulated route;
`FerrostarCore` will automatically add itself as a listener,
which will trigger updates.

#### TODO: Google Play Services-backed provider

## Configure an HTTP client

Before we configure the Ferrostar core, we need to set up an HTTP client.
We use the popular OkHttp library for this.
Here we’ve set up a client with a global timeout of 15 seconds.
Refer to the [OkHttp documentation](https://square.github.io/okhttp/) for further details on configuration.

```kotlin
private val httpClient = OkHttpClient.Builder()
    .callTimeout(Duration.ofSeconds(15))
    .build()
```

## Configure the `FerrostarCore` instance

We now have all the pieces we need to set up a `FerrostarCore` instance!
The `FerrostarCore` instance should live for at least the duration of a navigation session.
Bringing it all together, a typical init looks something like this:

```kotlin
private val core =
      FerrostarCore(
          valhallaEndpointURL = URL("https://api.stadiamaps.com/route/v1?api_key=YOUR-API-KEY"),
          profile = "bicycle",
          httpClient = httpClient,
          locationProvider = locationProvider,
      )
```

`FerrostarCore` exposes a number of convenience constructors for common cases,
such as using a Valhalla [Route Provider](./route-providers.md#bundled-support).

`FerrostarCore` automatically subscribes to location updates from the `LocationProvider`.

## Set up voice guidance

Ferrostar is able to process spoken instructions generated from some routing engines.
The `com.stadiamaps.ferrostar.core.SpokenInstructionObserver` interface
specifies haw to create your own observer.
A reference implementation is provided in the `AndroidTtsObserver` class,
which uses the text-to-speech engine built into Android.
PRs welcome for other popular services (ex: Amazon Polly;
note that some APIs also provide SSML instructions which work great with this!).

TODO (unsure where best to document the full setup until we see how it shakes out on iOS)

* Android Manifest
* Set the language
* Additional config (you have full control)

## Getting a route

TODO

## Starting a navigation session

TODO

## Using the `NavigationMapView`

TODO

## Demo app

We've put together a minimal [demo app](https://github.com/stadiamaps/ferrostar/tree/main/android/demo-app) with an example integration.

## Going deeper

This covers the basic “batteries included” configuration which works for simple apps.
But there’s a lot of room for customization!
Skip on over to the customization chapters that interest you.