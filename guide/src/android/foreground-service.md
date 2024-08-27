# Android Foreground Service

Ferrostar provides an Android foreground service that runs automatically during a trip.
Such a service is required to comply with Google's [Foreground Location Requirements](https://support.google.com/googleplay/android-developer/answer/9799150#Accessing%20location%20in%20the%20foreground).

## Overview

The Kotlin `ForegroundServiceManager` links `FerrostarCore` to the foreground service `FerrostarForegroundService` using a binder.
This technique allows us to bind the foreground service to a trip operated by the core.
When navigation starts, the foreground service is started and bound automatically.
Similarly, when we stop navigating, the core will stop the foreground and unbind it.

Note: you must call stop on Ferrostar core to stop the notification.
It does not automatically close out when the user arrives because location updates are not stopped.

### Setting Up Permissions

This feature uses the existing `AndroidManifest.xml` items in the Ferrostar core module.
These do not need to be added to your app.

```xml
    <!-- Foreground service permission -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />

    <application>
        <service
            android:name="com.stadiamaps.ferrostar.core.service.FerrostarForegroundService"
            android:foregroundServiceType="location"
            />
    </application>
```

Your app must still request `POST_NOTIFICATIONS` and `FOREGROUND_SERVICE_LOCATION` permissions on API 34+ (Upside Down Cake).
See the demo app's call to request these permissions in your composable app.

```kotlin
  val allPermissions =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.FOREGROUND_SERVICE_LOCATION)
      } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
      }

  val permissionsLauncher =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        // TODO: Handle permission fialures.
      }
```

### Using the Service Manager with `FerrostarCore`

In your app module, you'll need to pass in the `ForegroundServiceManager` as a parameter to construct `FerrostarCore`.
Here's how to do that with the included notification builder.

```kotlin
val foregroundServiceManager: ForegroundServiceManager = FerrostarForegroundServiceManager(appContext, DefaultForegroundNotificationBuilder(appContext))

val core =
        FerrostarCore(
            ...,
            foregroundServiceManager,
            ...)
```

The demo app shows this using a lazy initializer.

TODO: link the dependency injection example here once we have a doc for that.

### Customization

You can provide your own implementation of the `ForegroundNotificationBuilder` to customize the notification the foreground service publishes to users. 
To accomplish this, simply create a new `Notification` like the `DefaultForegroundNotificationBuilder` that implements the abstract `ForegroundNotificationBuilder`. 
Your class needs to create and build the foreground notification. This can include setting any required pending intents (e.g. `openPendingIntent`), 
portraying relevant information about the service to the user and so on.

```kotlin
class MyForegroundNotificationBuilder(
    context: Context
) : ForegroundNotificationBuilder(context) {

  override fun build(tripState: TripState?): Notification {
    if (channelId == null) {
      throw IllegalStateException("channelId must be set before building the notification.")
    }

    // Generate the notification builder. Note that channelId is set on newer versions of Android.
    // The channel is used to associate the notification in settings with the channel's title. This allows
    // a user to better understand the notification they're being presented in the android settings app.
    val builder: Notification.Builder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          Notification.Builder(context, channelId)
        } else {
          Notification.Builder(context)
        }

    // TODO: Build your notification off of the TripState here.


    // Set the open app pending intent. This example shows the case where the user tapping the notification will
    // open the app.
    builder.setContentIntent(openPendingIntent)

    // You can also use the provided `stopPendingIntent` which like Google Maps, Mapbox and others allows the user
    // to directly stop the navigation trip (and location updates) from the notification.

    return builder.build()
  }
}
```

When initializing the manager, all you need to do is change out the notification builder to your custom one.

```kotlin
val foregroundServiceManager: ForegroundServiceManager = FerrostarForegroundServiceManager(appContext, MyForegroundNotificationBuilder(appContext))
```

## Learn More

- [Understanding location in the background permissions](https://support.google.com/googleplay/android-developer/answer/9799150#Accessing%20location%20in%20the%20foreground)
- [Services](https://developer.android.com/develop/background-work/services)
- [Foreground service types are required](https://developer.android.com/about/versions/14/changes/fgs-types-required#use-cases)