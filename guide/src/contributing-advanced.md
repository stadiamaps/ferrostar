
## Android

### Using a Local Build of Maplibre-Compose

Ferrostar android depends heavily on functionality with Maplibre-Compose.

```
./gradlew assembleRelease
```

In both the Demo App and Maplibre UI build.gradle, replace libs.maplibre.compose with the following:

```groovy
//    api libs.maplibre.compose
    api 'org.maplibre.gl:android-sdk:10.3.0'
    api 'org.maplibre.gl:android-plugin-annotation-v9:2.0.2'
    implementation files('/{path}/{to}/maplibre-compose-playground/compose/build/outputs/aar/compose-release.aar')
```