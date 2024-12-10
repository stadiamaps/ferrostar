package expo.modules.ferrostar

import expo.modules.ferrostar.records.FerrostarCoreOptions
import expo.modules.ferrostar.records.NavigationOptions
import expo.modules.ferrostar.records.NavigationControllerConfig
import expo.modules.ferrostar.records.Route
import expo.modules.ferrostar.records.UserLocation
import expo.modules.ferrostar.records.Waypoint
import expo.modules.kotlin.functions.Coroutine
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class ExpoFerrostarModule : Module() {
    override fun definition() = ModuleDefinition {
        Name("ExpoFerrostar")

        View(ExpoFerrostarView::class) {
            Events(
                "onNavigationStateChange"
            )

            AsyncFunction("createRouteFromOsrm") { view: ExpoFerrostarView, osrmRoute: String, waypoints: String ->
                return@AsyncFunction view.createRouteFromOsrm(osrmRoute, waypoints)
            }

            AsyncFunction("getRoutes") Coroutine { view: ExpoFerrostarView, initialLocation: UserLocation, waypoints: List<Waypoint> ->
                return@Coroutine view.getRoutes(initialLocation, waypoints)
            }

            AsyncFunction("setPreviewRoute") { view: ExpoFerrostarView, route: Route ->
                view.setPreviewRoute(route)
            }

            AsyncFunction("startNavigation") { view: ExpoFerrostarView, route: Route, options: NavigationControllerConfig? ->
                view.startNavigation(route, options)
            }

            AsyncFunction("stopNavigation") { view: ExpoFerrostarView, stopLocationUpdates: Boolean? ->
                view.stopNavigation(stopLocationUpdates)
            }

            AsyncFunction("replaceRoute") { view: ExpoFerrostarView, route: Route, options: NavigationControllerConfig? ->
                view.replaceRoute(route, options)
            }

            AsyncFunction("advanceToNextStep") { view: ExpoFerrostarView -> view.advanceToNextStep() }

            Prop("navigationOptions") { view: ExpoFerrostarView, options: NavigationOptions ->
                view.setNavigationOptions(options)
            }

            Prop("coreOptions") { view: ExpoFerrostarView, options: FerrostarCoreOptions ->
                view.setCoreOptions(options)
            }
        }
    }
}
