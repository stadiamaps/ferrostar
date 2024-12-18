import { ExpoConfig } from "expo/config";
import {
  AndroidConfig,
  ConfigPlugin,
  withAndroidManifest,
  withAppBuildGradle,
} from "@expo/config-plugins";
import { ManifestUsesPermission } from "@expo/config-plugins/build/android/Manifest";

const withFerrostar: ConfigPlugin = (config, props) => {
  const androidManifestConfig = withAndroidManifest(config, (config) => {
    const mainApplication = AndroidConfig.Manifest.getMainApplicationOrThrow(
      config.modResults
    );

    const manifest = config.modResults.manifest;

    const permissions = manifest["uses-permission"] || [];
    const newPermissions: ManifestUsesPermission[] = [
      { $: { "android:name": "android.permission.ACCESS_COARSE_LOCATION" } },
      { $: { "android:name": "android.permission.ACCESS_FINE_LOCATION" } },
      { $: { "android:name": "android.permission.FOREGROUND_SERVICE" } },
      {
        $: {
          "android:name": "android.permission.FOREGROUND_SERVICE_LOCATION",
        },
      },
      { $: { "android:name": "android.permission.POST_NOTIFICATIONS" } },
    ];

    // We need to remove the existing permissions
    for (const permission of permissions) {
      if (
        permission.$["android:name"] ===
          "android.permission.ACCESS_COARSE_LOCATION" ||
        permission.$["android:name"] ===
          "android.permission.ACCESS_FINE_LOCATION" ||
        permission.$["android:name"] ===
          "android.permission.FOREGROUND_SERVICE" ||
        permission.$["android:name"] ===
          "android.permission.FOREGROUND_SERVICE_LOCATION" ||
        permission.$["android:name"] === "android.permission.POST_NOTIFICATIONS"
      ) {
        continue;
      }
      newPermissions.push(permission);
    }

    manifest["uses-permission"] = newPermissions;

    // Add the ferrostar service
    if (!mainApplication.service) {
      mainApplication.service = [];
    }

    // We need to remove the existing service
    for (const service of mainApplication.service) {
      if (
        service.$["android:name"] ===
        "com.stadiamaps.ferrostar.core.service.FerrostarForegroundService"
      ) {
        // Purge the service
        mainApplication.service = mainApplication.service.filter(
          (s) =>
            s.$["android:name"] !==
            "com.stadiamaps.ferrostar.core.service.FerrostarForegroundService"
        );
      }
    }

    mainApplication.service.push({
      $: {
        "android:name":
          "com.stadiamaps.ferrostar.core.service.FerrostarForegroundService",
        // @ts-ignore
        "android:foregroundServiceType": "location",
      },
    });

    return config;
  });

  return withAppBuildGradle(androidManifestConfig, (config) => {
    const androidPattern = "\nandroid {\n";
    const androidIndex = config.modResults.contents.indexOf(androidPattern);
    const androidPivot = androidIndex + androidPattern.length + 1;
    config.modResults.contents =
      config.modResults.contents.slice(0, androidPivot) +
      "   compileOptions {\n        coreLibraryDesugaringEnabled true\n    }\n\n " +
      config.modResults.contents.slice(androidPivot);

    const dependenciesPattern = "\ndependencies {\n";
    const dependenciesIndex =
      config.modResults.contents.indexOf(dependenciesPattern);
    const dependenciesPivot =
      dependenciesIndex + dependenciesPattern.length + 1;
    config.modResults.contents =
      config.modResults.contents.slice(0, dependenciesPivot) +
      '   coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")\n\n ' +
      config.modResults.contents.slice(dependenciesPivot);

    return config;
  });
};

export default withFerrostar;
