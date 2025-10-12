use crate::models::RouteStep;
use crate::navigation_controller::models::WaypointAdvanceMode;
use crate::navigation_controller::TripState;
use crate::navigation_controller::Waypoint;
use geo::{Closest, Distance, Haversine, HaversineClosestPoint, Point};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum WaypointCheckEvent {
    LocationUpdated,
    StepAdvanced,
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
    /// * `current_step` - The current route step being navigated.
    /// * `event` - The event that triggered the waypoint check.
    pub fn has_new_waypoints(
        &self,
        state: &TripState,
        current_step: &RouteStep,
        event: WaypointCheckEvent,
    ) -> Option<Vec<Waypoint>> {
        match state {
            TripState::Navigating {
                ref user_location,
                ref remaining_waypoints,
                ..
            } => {
                match self.mode {
                    WaypointAdvanceMode::WaypointWithinRange(range) => {
                        // Only advance waypoints if there are more than 1 remaining
                        // (never remove the final destination waypoint)
                        if remaining_waypoints.len() <= 1 {
                            return None;
                        }

                        remaining_waypoints.first().and_then(|waypoint| {
                            let current_location: Point = user_location.coordinates.into();
                            let next_waypoint: Point = waypoint.coordinate.into();
                            let distance = Haversine.distance(current_location, next_waypoint);
                            if distance < range {
                                // Slice the remaining waypoints starting from the second element
                                Some(remaining_waypoints[1..].to_vec())
                            } else {
                                None
                            }
                        })
                    }
                    WaypointAdvanceMode::WaypointAlongAdvancingStep(range) => {
                        if event == WaypointCheckEvent::StepAdvanced {
                            let step_linestring = current_step.get_linestring();
                            let mut filtered_waypoints: Vec<Waypoint> = remaining_waypoints
                                .iter()
                                .filter(|waypoint| {
                                    let waypoint_point: Point = waypoint.coordinate.into();
                                    !self.is_waypoint_within_range_of_linestring(
                                        &waypoint_point,
                                        &step_linestring,
                                        range,
                                    )
                                })
                                .cloned()
                                .collect();

                            // Never remove the last waypoint (destination)
                            if filtered_waypoints.is_empty() && !remaining_waypoints.is_empty() {
                                filtered_waypoints
                                    .push(remaining_waypoints.last().unwrap().clone());
                            }

                            if filtered_waypoints.len() != remaining_waypoints.len() {
                                Some(filtered_waypoints)
                            } else {
                                None
                            }
                        } else {
                            None
                        }
                    }
                }
            }
            TripState::Complete { .. } | TripState::Idle { .. } => None,
        }
    }

    /// Helper function to check if a waypoint is within range of any point on a linestring
    pub(crate) fn is_waypoint_within_range_of_linestring(
        &self,
        waypoint: &Point,
        linestring: &geo::LineString,
        range: f64,
    ) -> bool {
        match linestring.haversine_closest_point(waypoint) {
            Closest::Intersection(closest_point) | Closest::SinglePoint(closest_point) => {
                Haversine.distance(*waypoint, closest_point) < range
            }
            Closest::Indeterminate => false,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::models::{GeographicCoordinate, WaypointKind};
    use crate::navigation_controller::test_helpers::{
        gen_route_step_with_coords, get_navigating_trip_state,
    };
    use geo::{coord, Destination};
    use proptest::prelude::*;
    use std::time::SystemTime;

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
            user_lat in -85.0..85.0,
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

            // This waypoint mode doesn't care about the current step.
            let current_step = gen_route_step_with_coords(vec![
                coord! { x: user_lng, y: user_lat },
                coord! { x: user_lng + 0.01, y: user_lat + 0.01 },
            ]);

            let state = get_navigating_trip_state(
                user_location,
                current_step.clone(),
                waypoints.clone(),
            );

            let result = checker.has_new_waypoints(
                &state,
                &current_step,
                WaypointCheckEvent::LocationUpdated
            );

            // Should always advance the first waypoint since offsets are small and range is large
            prop_assert!(result.is_some());
            let new_waypoints = result.unwrap();
            prop_assert_eq!(new_waypoints.len(), waypoints.len() - 1);
            // Verify it's the correct remaining waypoints
            for (i, waypoint) in new_waypoints.iter().enumerate() {
                prop_assert_eq!(*waypoint, waypoints[i + 1]);
            }
        }

        /// Test WaypointWithinRange mode out of range, don't advance
        #[test]
        fn test_waypoint_within_range_never_advances(
            user_lng in -179.0..179.0,
            user_lat in -84.0..84.0,
            range_meters in 10.0..100.0,
            waypoint_range_meters in 101.0..5000.0,
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
                waypoint_range_meters,
                num_waypoints,
            );

            let current_step = gen_route_step_with_coords(vec![
                coord! { x: user_lng, y: user_lat },
                coord! { x: user_lng + 0.01, y: user_lat + 0.01 },
            ]);

            let state = get_navigating_trip_state(
                user_location,
                current_step.clone(),
                waypoints,
            );

            let result = checker.has_new_waypoints(
                &state,
                &current_step,
                WaypointCheckEvent::LocationUpdated
            );

            // Should never advance waypoints when they're guaranteed to be far away
            prop_assert!(result.is_none());
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
                current_step.clone(),
                waypoints.clone(),
            );

            let result = checker.has_new_waypoints(
                &state,
                &current_step,
                WaypointCheckEvent::StepAdvanced
            );

            // Should always advance since intermediate waypoints are very close to step line
            prop_assert!(result.is_some());
            let new_waypoints = result.unwrap();

            // Should only have the destination waypoint remaining
            prop_assert_eq!(new_waypoints.len(), 1);
            prop_assert_eq!(new_waypoints[0], waypoints.last().unwrap().clone());
        }

        /// Test WaypointAlongAdvancingStep mode out of range of step, should never advance
        #[test]
        fn test_waypoint_along_step_never_advances(
            step_start_lng in -179.0..179.0,
            step_start_lat in -84.0..84.0,
            step_end_lng in -179.0..179.0,
            step_end_lat in -84.0..84.0,
            range_meters in 10.0..100.0,
            waypoint_range_meters in 101.0..10000.0, // Distance from step line in meters (guaranteed out of range)
            num_waypoints in 2usize..5,
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

            // Create waypoints far from the step line (guaranteed out of range)
            let mid_lng = (step_start_lng + step_end_lng) / 2.0;
            let mid_lat = (step_start_lat + step_end_lat) / 2.0;

            let waypoints = create_waypoints_at_distance(
                mid_lng,
                mid_lat,
                waypoint_range_meters,
                num_waypoints,
            );

            let state = get_navigating_trip_state(
                user_location,
                current_step.clone(),
                waypoints,
            );

            let result = checker.has_new_waypoints(
                &state,
                &current_step,
                WaypointCheckEvent::StepAdvanced
            );

            // Should never advance waypoints when they're guaranteed to be far from step
            prop_assert!(result.is_none());
        }

        /// Test that single waypoints (destinations) are never advanced in WaypointWithinRange mode
        #[test]
        fn test_single_waypoint_never_advanced_within_range(
            user_lng in -180.0..180.0,
            user_lat in -85.0..85.0,
            range_meters in 5.0..50.0,
            waypoint_range_meters in 5.0..50.0, // Match to range.
        ) {
            let checker = WaypointAdvanceChecker {
                mode: WaypointAdvanceMode::WaypointWithinRange(range_meters),
            };

            let user_location = create_user_location(user_lng, user_lat);

            // Create single waypoint close to user (within range)
            let waypoints = create_waypoints_at_distance(
                user_lng,
                user_lat,
                waypoint_range_meters,
                1,
            );

            let current_step = gen_route_step_with_coords(vec![
                coord! { x: user_lng, y: user_lat },
                coord! { x: user_lng + 0.01, y: user_lat + 0.01 },
            ]);

            let state = get_navigating_trip_state(
                user_location,
                current_step.clone(),
                waypoints.clone(),
            );

            let result = checker.has_new_waypoints(
                &state,
                &current_step,
                WaypointCheckEvent::LocationUpdated
            );

            // Should NEVER advance a single waypoint (destination), even if within range
            prop_assert!(result.is_none());
        }

        /// Test that single waypoints (destinations) are never advanced in WaypointAlongAdvancingStep mode
        #[test]
        fn test_single_waypoint_never_advanced_along_step(
            step_start_lng in -180.0..180.0,
            step_start_lat in -85.0..85.0,
            step_end_lng in -180.0..180.0,
            step_end_lat in -85.0..85.0,
            range_meters in 50.0..1000.0, // Detection range in meters
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

            // Create single waypoint very close to the step line (definitely within range)
            let mid_lng = (step_start_lng + step_end_lng) / 2.0;
            let mid_lat = (step_start_lat + step_end_lat) / 2.0;
            // Use 10 meters from midpoint to guarantee it's within range
            let waypoints = create_waypoints_at_distance(mid_lng, mid_lat, 10.0, 1);

            let state = get_navigating_trip_state(
                user_location,
                current_step.clone(),
                waypoints.clone(),
            );

            let result = checker.has_new_waypoints(
                &state,
                &current_step,
                WaypointCheckEvent::StepAdvanced
            );

            // Should never advance a single waypoint (destination), even if on the step line
            prop_assert!(result.is_none());
        }
    }
}
