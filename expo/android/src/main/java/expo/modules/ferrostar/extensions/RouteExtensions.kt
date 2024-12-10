package expo.modules.ferrostar.extensions

import expo.modules.ferrostar.records.BoundingBox
import expo.modules.ferrostar.records.GeographicCoordinate
import expo.modules.ferrostar.records.LaneInfo
import expo.modules.ferrostar.records.ManeuverModifier
import expo.modules.ferrostar.records.ManeuverType
import expo.modules.ferrostar.records.RouteStep
import expo.modules.ferrostar.records.SpokenInstruction
import expo.modules.ferrostar.records.VisualInstruction
import expo.modules.ferrostar.records.VisualInstructionContent
import expo.modules.ferrostar.records.Waypoint
import expo.modules.ferrostar.records.WaypointKind
import uniffi.ferrostar.Route

fun Route.Companion.toExpoRoute(route: Route): expo.modules.ferrostar.records.Route {
    val expoRoute = expo.modules.ferrostar.records.Route()
    val bbox = BoundingBox()
    val northEast = GeographicCoordinate()
    northEast.lat = route.bbox.ne.lat
    northEast.lng = route.bbox.ne.lng
    val southWest = GeographicCoordinate()
    southWest.lat = route.bbox.sw.lat
    southWest.lng = route.bbox.sw.lng
    bbox.ne = northEast
    bbox.sw = southWest

    expoRoute.bbox = bbox

    expoRoute.waypoints =
        route.waypoints.map { point ->
            val waypoint = Waypoint()
            val coordinate = GeographicCoordinate()
            coordinate.lat = point.coordinate.lat
            coordinate.lng = point.coordinate.lng
            waypoint.coordinate = coordinate
            waypoint.kind = WaypointKind.valueOf(point.kind.name)

            waypoint
        }

    expoRoute.steps =
        route.steps.map { step ->
            val routeStep = RouteStep()

            routeStep.distance = step.distance
            routeStep.duration = step.duration
            routeStep.roadName = step.roadName
            routeStep.annotations = step.annotations
            routeStep.instruction = step.instruction

            routeStep.geometry =
                step.geometry.map { point ->
                    val coordinate = GeographicCoordinate()
                    coordinate.lat = point.lat
                    coordinate.lng = point.lng

                    coordinate
                }

            routeStep.visualInstructions =
                step.visualInstructions.map { instruction ->
                    val visualInstruction = VisualInstruction()
                    val primaryContent = VisualInstructionContent()

                    primaryContent.text = instruction.primaryContent.text
                    primaryContent.maneuverType = instruction.primaryContent.maneuverType?.let {
                        ManeuverType.valueOf(it.name)
                    }
                    primaryContent.maneuverModifier = instruction.primaryContent.maneuverModifier?.let {
                        ManeuverModifier.valueOf(it.name)
                    }
                    primaryContent.roundaboutExitDegrees = instruction.primaryContent.roundaboutExitDegrees?.toInt()
                    primaryContent.laneInfo = instruction.primaryContent.laneInfo?.map { currentLaneInfo ->
                        val laneInfo = LaneInfo()

                        laneInfo.active = currentLaneInfo.active
                        laneInfo.directions = currentLaneInfo.directions
                        laneInfo.activeDirection = currentLaneInfo.activeDirection

                        laneInfo
                    }

                    visualInstruction.primaryContent = primaryContent

                    if (instruction.secondaryContent != null) {
                        val secondaryContent = VisualInstructionContent()
                        secondaryContent.text = instruction.secondaryContent!!.text
                        secondaryContent.maneuverType = instruction.secondaryContent!!.maneuverType?.let {
                            ManeuverType.valueOf(it.name)
                        }
                        secondaryContent.maneuverModifier = instruction.secondaryContent!!.maneuverModifier?.let {
                            ManeuverModifier.valueOf(it.name)
                        }
                        secondaryContent.roundaboutExitDegrees = instruction.secondaryContent!!.roundaboutExitDegrees?.toInt()
                        secondaryContent.laneInfo =
                            instruction.secondaryContent!!.laneInfo?.map { currentLaneInfo ->
                                val laneInfo = LaneInfo()

                                laneInfo.active = currentLaneInfo.active
                                laneInfo.directions = currentLaneInfo.directions
                                laneInfo.activeDirection = currentLaneInfo.activeDirection

                                laneInfo
                            }

                        visualInstruction.secondaryContent = secondaryContent
                    }

                    if (instruction.subContent != null) {
                        val subContent = VisualInstructionContent()
                        subContent.text = instruction.subContent!!.text
                        subContent.maneuverType = instruction.subContent!!.maneuverType?.let {
                            ManeuverType.valueOf(it.name)
                        }
                        subContent.maneuverModifier = instruction.subContent!!.maneuverModifier?.let {
                            ManeuverModifier.valueOf(it.name)
                        }
                        subContent.roundaboutExitDegrees = instruction.subContent!!.roundaboutExitDegrees?.toInt()
                        subContent.laneInfo =
                            instruction.subContent!!.laneInfo?.map { currentLaneInfo ->
                                val laneInfo = LaneInfo()

                                laneInfo.active = currentLaneInfo.active
                                laneInfo.directions = currentLaneInfo.directions
                                laneInfo.activeDirection = currentLaneInfo.activeDirection

                                laneInfo
                            }

                        visualInstruction.subContent = subContent
                    }

                    visualInstruction.triggerDistanceBeforeManeuver = instruction.triggerDistanceBeforeManeuver

                    visualInstruction
                }

            routeStep.spokenInstructions =
                step.spokenInstructions.map { instruction ->
                    val spokenInstruction = SpokenInstruction()
                    spokenInstruction.text = instruction.text
                    spokenInstruction.ssml = instruction.ssml
                    spokenInstruction.utteranceId = instruction.utteranceId.toString()
                    spokenInstruction.triggerDistanceBeforeManeuver =
                        instruction.triggerDistanceBeforeManeuver

                    spokenInstruction
                }

            routeStep
        }

    expoRoute.distance = route.distance
    expoRoute.geometry =
        route.geometry.map { point ->
            val coordinate = GeographicCoordinate()
            coordinate.lat = point.lat
            coordinate.lng = point.lng

            coordinate
        }

    return expoRoute
}