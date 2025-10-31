use crate::algorithms::deviation_from_line;
use crate::models::RouteStep;
use crate::navigation_controller::models::WaypointAdvanceMode;
use crate::navigation_controller::TripState;
use crate::navigation_controller::Waypoint;
use geo::{Distance, Haversine, Point};

#[derive(Debug, Clone, PartialEq)]
pub(crate) enum WaypointCheckEvent {
    LocationUpdated,
    StepAdvanced(RouteStep),
}

#[derive(Debug, Clone, PartialEq)]
pub(crate) enum WaypointAdvanceResult {
    Unchanged,
    Changed(Vec<Waypoint>),
}

pub(crate) struct WaypointAdvanceChecker {
    pub mode: WaypointAdvanceMode,
}

impl WaypointAdvanceChecker {
    /// Returns a new set of waypoints if they are changed.
    ///
    /// # Parameters
    ///
    /// * `state` - The current trip state.
    /// * `event` - The event that triggered the waypoint check.
    pub fn get_new_waypoints(
        &self,
        state: &TripState,
        event: WaypointCheckEvent,
    ) -> WaypointAdvanceResult {
        match state {
            TripState::Navigating {
                ref user_location,
                ref remaining_waypoints,
                ..
            } => {
                match self.mode {
                    WaypointAdvanceMode::WaypointWithinRange(range) => {
                        if event != WaypointCheckEvent::LocationUpdated {
                            return WaypointAdvanceResult::Unchanged;
                        }

                        // Only advance waypoints if there are more than 1 remaining
                        // (never remove the final destination waypoint)
                        if remaining_waypoints.len() <= 1 {
                            return WaypointAdvanceResult::Unchanged;
                        }

                        remaining_waypoints.first().map_or(
                            WaypointAdvanceResult::Unchanged,
                            |waypoint| {
                                let current_location: Point = user_location.coordinates.into();
                                let next_waypoint: Point = waypoint.coordinate.into();
                                let distance = Haversine.distance(current_location, next_waypoint);
                                if distance < range {
                                    // Slice the remaining waypoints starting from the second element
                                    WaypointAdvanceResult::Changed(
                                        remaining_waypoints[1..].to_vec(),
                                    )
                                } else {
                                    WaypointAdvanceResult::Unchanged
                                }
                            },
                        )
                    }
                    WaypointAdvanceMode::WaypointAlongAdvancingStep(range) => {
                        let WaypointCheckEvent::StepAdvanced(current_step) = event else {
                            return WaypointAdvanceResult::Unchanged;
                        };

                        let step_linestring = current_step.get_linestring();
                        let mut filtered_waypoints: Vec<Waypoint> = remaining_waypoints
                            .iter()
                            .filter(|waypoint| {
                                let waypoint_point: Point = waypoint.coordinate.into();
                                let is_beyond_range =
                                    deviation_from_line(&waypoint_point, &step_linestring)
                                        .is_some_and(|diff| diff > range);
                                // Only keep waypoints that are beyond the range
                                is_beyond_range
                            })
                            .cloned()
                            .collect();

                        // Never remove the last waypoint (destination)
                        if filtered_waypoints.is_empty() && !remaining_waypoints.is_empty() {
                            filtered_waypoints.push(remaining_waypoints.last().unwrap().clone());
                        }

                        if filtered_waypoints.len() != remaining_waypoints.len() {
                            WaypointAdvanceResult::Changed(filtered_waypoints)
                        } else {
                            WaypointAdvanceResult::Unchanged
                        }
                    }
                }
            }
            TripState::Complete { .. } | TripState::Idle { .. } => WaypointAdvanceResult::Unchanged,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::deviation_detection::RouteDeviation;
    use crate::models::{GeographicCoordinate, WaypointKind};
    use crate::navigation_controller::test_helpers::{
        gen_route_step_with_coords, get_navigating_trip_state,
    };
    use geo::{coord, Destination};
    use proptest::prelude::*;

    #[cfg(all(test, feature = "std", not(feature = "web-time")))]
    use std::time::SystemTime;

    #[cfg(all(test, feature = "web-time"))]
    use web_time::SystemTime;

    fn create_user_location(lng: f64, lat: f64) -> crate::models::UserLocation {
        crate::models::UserLocation {
            coordinates: GeographicCoordinate { lng, lat },
            horizontal_accuracy: 5.0,
            course_over_ground: None,
            timestamp: SystemTime::now(),
            speed: None,
        }
    }

    fn create_waypoint(lng: f64, lat: f64) -> Waypoint {
        Waypoint {
            coordinate: GeographicCoordinate { lng, lat },
            kind: WaypointKind::Break,
            properties: None,
        }
    }

    /// Creates waypoints at specified distances from the user location
    ///
    /// # Parameters
    ///
    /// * `user_lng` - User's longitude
    /// * `user_lat` - User's latitude
    /// * `waypoint_range_meters` - Distance in meters from user to place waypoints
    /// * `num_waypoints` - Number of waypoints to create
    fn create_waypoints_at_distance(
        user_lng: f64,
        user_lat: f64,
        waypoint_range_meters: f64,
        num_waypoints: usize,
    ) -> Vec<Waypoint> {
        let mut waypoints = Vec::new();
        let origin_point = Point::new(user_lng, user_lat);

        for i in 0..num_waypoints {
            let bearing = (i as f64 * 360.0) / num_waypoints as f64;
            let destination = Haversine.destination(origin_point, bearing, waypoint_range_meters);
            waypoints.push(create_waypoint(destination.x(), destination.y()));
        }

        waypoints
    }

    proptest! {
        /// Test WaypointWithinRange mode within range
        #[test]
        fn test_waypoint_within_range_always_advances(
            user_lng in -180.0..180.0,
            user_lat in -90.0..90.0,
            range_meters in 200.0..1000.0,
            waypoint_range_meters in 10.0..199.9,
            num_waypoints in 3usize..10,
        ) {
            let checker = WaypointAdvanceChecker {
                mode: WaypointAdvanceMode::WaypointWithinRange(range_meters),
            };

            let user_location = create_user_location(user_lng, user_lat);

            // Create waypoints at specified distance from user (guaranteed within range)
            let waypoints = create_waypoints_at_distance(
                user_lng,
                user_lat,
                waypoint_range_meters,
                num_waypoints,
            );

            let state = get_navigating_trip_state(
                user_location,
                vec![],
                waypoints.clone(),
                RouteDeviation::NoDeviation,
            );

            let result = checker.get_new_waypoints(
                &state,
                WaypointCheckEvent::LocationUpdated
            );

            // Should always advance the first waypoint since offsets are small and range is large
            let new_waypoints = if let WaypointAdvanceResult::Changed(waypoints) = result {
                waypoints
            } else {
                prop_assert!(false, "Expected WaypointAdvanceResult::Changed, got {:?}", result);
                unreachable!()
            };
            prop_assert_eq!(new_waypoints.len(), waypoints.len() - 1);
            // Verify it's the correct remaining waypoints
            for (i, waypoint) in new_waypoints.iter().enumerate() {
                prop_assert_eq!(waypoint, &waypoints[i + 1]);
            }
        }

        /// Test WaypointWithinRange mode out of range, don't advance
        #[test]
        fn test_waypoint_within_range_never_advances(
            user_lng in -180.0..180.0,
            user_lat in -90.0..90.0,
            range_meters in 10.0..100.0,
            diff in 0.1..5000.0,
            num_waypoints in 2usize..5,
        ) {
            let checker = WaypointAdvanceChecker {
                mode: WaypointAdvanceMode::WaypointWithinRange(range_meters),
            };

            let user_location = create_user_location(user_lng, user_lat);

            // Create waypoints at specified distance from user (guaranteed out of range)
            let waypoints = create_waypoints_at_distance(
                user_lng,
                user_lat,
                range_meters + diff,
                num_waypoints,
            );

            let state = get_navigating_trip_state(
                user_location,
                vec![],
                waypoints,
                RouteDeviation::NoDeviation,
            );

            let result = checker.get_new_waypoints(
                &state,
                WaypointCheckEvent::LocationUpdated
            );

            // Should never advance waypoints when they're guaranteed to be far away
            prop_assert!(matches!(result, WaypointAdvanceResult::Unchanged));
        }

        /// Test WaypointAlongAdvancingStep mode within range of step.
        #[test]
        fn test_waypoint_along_step_always_advances(
            step_start_lng in -10.0..10.0,
            step_start_lat in -10.0..10.0,
            range_meters in 200.0..1000.0,
            waypoint_range_meters in 10.0..199.9,
            num_waypoints in 3usize..6,
        ) {
            let checker = WaypointAdvanceChecker {
                mode: WaypointAdvanceMode::WaypointAlongAdvancingStep(range_meters),
            };

            let user_location = create_user_location(step_start_lng, step_start_lat);

            // Create a simple short step line (100 meters)
            let step_end_lng = step_start_lng + 0.001; // ~111 meters east
            let step_end_lat = step_start_lat;

            let current_step = gen_route_step_with_coords(vec![
                coord! { x: step_start_lng, y: step_start_lat },
                coord! { x: step_end_lng, y: step_end_lat },
            ]);

            // Place intermediate waypoints close to the step midpoint (guaranteed within range)
            let mid_lng = (step_start_lng + step_end_lng) / 2.0;
            let mid_lat = (step_start_lat + step_end_lat) / 2.0;

            // Create intermediate waypoints at specified distance from step midpoint
            let waypoints = create_waypoints_at_distance(
                mid_lng,
                mid_lat,
                waypoint_range_meters,
                num_waypoints - 1,
            );

            let state = get_navigating_trip_state(
                user_location,
                vec![],
                waypoints.clone(),
                RouteDeviation::NoDeviation,
            );

            let result = checker.get_new_waypoints(
                &state,
                WaypointCheckEvent::StepAdvanced(current_step.clone())
            );

            // Should always advance since intermediate waypoints are very close to step line
            let new_waypoints = if let WaypointAdvanceResult::Changed(waypoints) = result {
                waypoints
            } else {
                prop_assert!(false, "Expected WaypointAdvanceResult::Changed, got {:?}", result);
                unreachable!()
            };

            // Should only have the destination waypoint remaining
            prop_assert_eq!(new_waypoints.len(), 1);
            prop_assert_eq!(&new_waypoints[0], waypoints.last().unwrap());
        }

        /// Test WaypointAlongAdvancingStep mode out of range of step, should never advance
        #[test]
        fn test_waypoint_along_step_never_advances(
            step_start_lng in -180.0..180.0,
            step_start_lat in -90.0..90.0,
            range_meters in 0.1..1000.0,
            diff_meters in 112.0..10000.0, // Has to exceed the length of the step (~111 meters).
            num_waypoints in 2usize..5,
        ) {
            let checker = WaypointAdvanceChecker {
                mode: WaypointAdvanceMode::WaypointAlongAdvancingStep(range_meters),
            };

            let user_location = create_user_location(step_start_lng, step_start_lat);

            let step_end_lng = step_start_lng + 0.001; // ~111 meters east
            let step_end_lat = step_start_lat;

            let current_step = gen_route_step_with_coords(vec![
                coord! { x: step_start_lng, y: step_start_lat },
                coord! { x: step_end_lng, y: step_end_lat },
            ]);

            let mid_lng = (step_start_lng + step_end_lng) / 2.0;
            let mid_lat = (step_start_lat + step_end_lat) / 2.0;

            // Create waypoints outside of the range by diff_meters
            let waypoints = create_waypoints_at_distance(
                mid_lng,
                mid_lat,
                range_meters + diff_meters,
                num_waypoints,
            );

            let state = get_navigating_trip_state(
                user_location,
                vec![],
                waypoints,
                RouteDeviation::NoDeviation,
            );

            let result = checker.get_new_waypoints(
                &state,
                WaypointCheckEvent::StepAdvanced(current_step)
            );

            // Should never advance waypoints when they're guaranteed to be far from step
            prop_assert!(matches!(result, WaypointAdvanceResult::Unchanged));
        }

        /// Test that single waypoints (destinations) are never advanced in WaypointWithinRange mode
        #[test]
        fn test_single_waypoint_never_advanced_within_range(
            user_lng in -180.0..180.0,
            user_lat in -90.0..90.0,
            range_meters in 0.1..1000.0,
            diff_meters in 0.0..1000.0, // diff can be within or outside of range.
        ) {
            let checker = WaypointAdvanceChecker {
                mode: WaypointAdvanceMode::WaypointWithinRange(range_meters),
            };

            let user_location = create_user_location(user_lng, user_lat);

            // Create waypoints on the user's location or any distance from it.
            let waypoints = create_waypoints_at_distance(user_lng, user_lat, diff_meters, 1);

            let state = get_navigating_trip_state(
                user_location,
                vec![],
                waypoints.clone(),
                RouteDeviation::NoDeviation,
            );

            let result = checker.get_new_waypoints(
                &state,
                WaypointCheckEvent::LocationUpdated
            );

            // Should NEVER advance a single waypoint (destination), even if within range
            prop_assert!(result == WaypointAdvanceResult::Unchanged);
        }

        /// Test that single waypoints (destinations) are never advanced in WaypointAlongAdvancingStep mode
        #[test]
        fn test_single_waypoint_never_advanced_along_step(
            step_start_lng in -180.0..180.0,
            step_start_lat in -90.0..90.0,
            step_end_lng in -180.0..180.0,
            step_end_lat in -90.0..90.0,
            range_meters in 0.1..1000.0,
            diff_meters in 0.0..1000.0, // diff can be within or outside of range.
        ) {
            let checker = WaypointAdvanceChecker {
                mode: WaypointAdvanceMode::WaypointAlongAdvancingStep(range_meters),
            };

            let user_location = create_user_location(step_start_lng, step_start_lat);

            // Create a step with a line
            let current_step = gen_route_step_with_coords(vec![
                coord! { x: step_start_lng, y: step_start_lat },
                coord! { x: step_end_lng, y: step_end_lat },
            ]);

            let mid_lng = (step_start_lng + step_end_lng) / 2.0;
            let mid_lat = (step_start_lat + step_end_lat) / 2.0;

            // Create waypoints within and outside of the range of the step's midpoint
            let waypoints = create_waypoints_at_distance(mid_lng, mid_lat, diff_meters, 1);

            let state = get_navigating_trip_state(
                user_location,
                vec![],
                waypoints.clone(),
                RouteDeviation::NoDeviation,
            );

            let result = checker.get_new_waypoints(
                &state,
                WaypointCheckEvent::StepAdvanced(current_step)
            );

            // Should never advance a single waypoint (destination), even if on the step line
            prop_assert!(result == WaypointAdvanceResult::Unchanged);
        }
    }
}
