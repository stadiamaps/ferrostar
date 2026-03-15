# Implementing Android Auto (Car App)

Ferrostar provides tooling to construct an Android Auto navigation app. The Demo App's
auto directory is a good place to review this.

## Basic Setup


### Android Manifest & XML

1. Add the navigation service to your apps manifest [`AndroidManifest.xml#L52`](android/demo-app/src/main/AndroidManifest.xml#L52)
2. Set the minimum car app version in the manifest [`AndroidManifest.xml#L29`](android/demo-app/src/main/AndroidManifest.xml#L29). You can configure this based on your
specific needs. E.g. based on the features you use elsewhere in your car app 
implementation.
3. Add the automotive app descriptor [`automotive_app_desc.xml`](android/demo-app/src/main/res/xml/automotive_app_desc.xml). Link this in your manifest [`AndroidManifest.xml#L32`](android/demo-app/src/main/AndroidManifest.xml#L32)

### Car App Service



### Car App Screen



## Requirements

Google has specific review guidelines for Android Auto navigation apps. You can 
find them here: [Car App Quality Guidelines](https://developer.android.com/docs/quality-guidelines/car-app-quality). 
Search for `NF` to find the navigation app specific guidelines.

This document summarizes the navigation app guidelines (as of March 2026) and 
provides guidance on how Ferrostar can be used to implement them in your 
Android Auto app.

### NF-1 - Turn by Turn Navigation

> The app must provide turn-by-turn navigation directions.

The Ferrostar `car.app` module provides the [`NavigationTemplateBuilder`](android/car-app/src/main/java/com/stadiamaps/ferrostar/car/app/template/NavigationTemplateBuilder.kt) 
which translates ferrostar's active navigation state into a navigation template for 
Android Auto.

### NF-2 - Only Map Content on the Surface (with Exceptions)

> The app draws only map content on the surface of the navigation templates. Text-based turn-by-turn directions, lane guidance, and estimated arrival time must be displayed on the relevant components of the navigation template. Additional information relevant to the drive, speed limit, road obstructions, etc., can be drawn on the safe area of the map.

The [CarAppNavigationView](android/ui-maplibre-car-app/src/main/java/com/stadiamaps/ferrostar/ui/maplibre/car/app/CarAppNavigationView.kt) implements this. There are
multiple runtime tools that ensure the current road name and speed limit are displayed
within the safe area of the screen.

### NF-3 - Turn by Turn Notifications

> When the app provides text-based turn-by-turn directions, it must also trigger navigation notifications. For more information, see 
[Turn-by-turn notifications](https://developer.android.com/training/cars/apps/navigation#turn-by-turn-notifications).

Ferrostar includes [`TurnByTurnNotificationManager`](android/car-app/src/main/java/com/stadiamaps/ferrostar/car/app/navigation/TurnByTurnNotificationManager.kt) 
for this purpose.

### NF-4 - Next Step to Cluster

> When the navigation app provides text-based turn-by-turn directions, it must send next-turn information to the vehicle’s cluster display. For more information, see [Navigation metadata](https://developer.android.com/training/cars/apps/navigation#navigation-metadata).

TODO: Implement.

### NF-5 - Don't Interfere if not Navigating

> The app must not provide turn-by-turn notifications, voice guidance, or cluster information when another navigation app is providing turn-by-turn instructions. For more information, see [Start, end, and stop navigation](https://developer.android.com/training/cars/apps/navigation#starting-ending-stopping-navigation).

TODO: Evaluate.

### NF-6 - App Must handle Navigation Intents

> The app must handle navigation requests from other apps. For more information, see [Support navigation intents](https://developer.android.com/training/cars/apps/navigation#support-navigation-intents).

### NF-7 - Test drive mode

> The app must provide a "test drive" mode that simulates driving. For more information, see [Simulate navigation](https://developer.android.com/training/cars/apps/navigation#simulating-navigation).

App must simulate navigation when auto drive is called. 

```sh
adb shell dumpsys activity service com.stadiamaps.ferrostar.auto.DemoCarAppService AUTO_DRIVE
```
