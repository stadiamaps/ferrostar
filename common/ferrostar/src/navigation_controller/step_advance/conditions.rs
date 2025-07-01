use std::sync::Arc;

use crate::{
    algorithms::{deviation_from_line, is_within_threshold_to_end_of_linestring},
    models::{RouteStep, UserLocation},
};
use geo::Point;

use super::{StepAdvanceCondition, StepAdvanceConditionJsConvertible, StepAdvanceResult};

#[cfg(feature = "wasm-bindgen")]
use serde::{Deserialize, Serialize};

#[cfg(feature = "wasm-bindgen")]
use tsify::Tsify;

#[cfg(feature = "wasm-bindgen")]
use super::JsStepAdvanceCondition;

// MARK: Manual

// We *could* implement Serialize for the major modes...

/// Never advances to the next step automatically;
/// requires calling [`NavigationController::advance_to_next_step`](super::NavigationController::advance_to_next_step).
///
/// You can use this to implement custom behaviors in external code.
#[derive(Debug, Copy, Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
pub struct ManualStepAdvance;

impl StepAdvanceCondition for ManualStepAdvance {
    #[allow(unused_variables)]
    fn should_advance_step(
        &self,
        user_location: UserLocation,
        current_step: RouteStep,
        next_step: Option<RouteStep>,
    ) -> StepAdvanceResult {
        StepAdvanceResult {
            should_advance: false,
            next_iteration: Arc::new(ManualStepAdvance),
        }
    }
}

impl StepAdvanceConditionJsConvertible for ManualStepAdvance {
    #[cfg(feature = "wasm-bindgen")]
    fn to_js(&self) -> JsStepAdvanceCondition {
        JsStepAdvanceCondition::Manual
    }
}

// MARK: Basic Conditions

/// Automatically advances when the user's location is close enough to the end of the step
#[derive(Debug, Copy, Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
pub struct DistanceToEndOfStep {
    /// Distance to the last waypoint in the step, measured in meters, at which to advance.
    pub distance: u16,
    /// The minimum required horizontal accuracy of the user location, in meters.
    /// Values larger than this cannot trigger a step advance.
    pub minimum_horizontal_accuracy: u16,
}

impl StepAdvanceCondition for DistanceToEndOfStep {
    #[allow(unused_variables)]
    fn should_advance_step(
        &self,
        user_location: UserLocation,
        current_step: RouteStep,
        next_step: Option<RouteStep>,
    ) -> StepAdvanceResult {
        let should_advance =
            if user_location.horizontal_accuracy > self.minimum_horizontal_accuracy.into() {
                false
            } else {
                is_within_threshold_to_end_of_linestring(
                    &user_location.into(),
                    &current_step.get_linestring(),
                    f64::from(self.distance),
                )
            };

        StepAdvanceResult {
            should_advance,
            next_iteration: Arc::new(DistanceToEndOfStep {
                distance: self.distance,
                minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
            }),
        }
    }
}

impl StepAdvanceConditionJsConvertible for DistanceToEndOfStep {
    #[cfg(feature = "wasm-bindgen")]
    fn to_js(&self) -> JsStepAdvanceCondition {
        JsStepAdvanceCondition::DistanceToEndOfStep {
            distance: self.distance,
            minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
        }
    }
}

/// Requires that the user be at least this far (distance in meters)
/// from the current route step.
///
/// This results in *delayed* advance,
/// but is more robust to spurious / unwanted step changes in scenarios including
/// self-intersecting routes (sudden jump to the next step)
/// and pauses at intersections (advancing too soon before the maneuver is complete).
///
/// NOTE! This may be less robust to things like short steps, out and backs and U-turns,
/// where this may eagerly exit a current step before the user has traversed it if the start
/// the step within range of the end.
#[derive(Debug, Copy, Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
pub struct DistanceFromStep {
    /// The minimum the distance the user must have travelled from the step's polyline.
    pub distance: u16,
    /// The minimum required horizontal accuracy of the user location, in meters.
    /// Values larger than this cannot ever trigger a step advance.
    pub minimum_horizontal_accuracy: u16,
}

impl StepAdvanceCondition for DistanceFromStep {
    #[allow(unused_variables)]
    fn should_advance_step(
        &self,
        user_location: UserLocation,
        current_step: RouteStep,
        next_step: Option<RouteStep>,
    ) -> StepAdvanceResult {
        // Exit early if the user location is not accurate enough.
        let should_advance =
            if user_location.horizontal_accuracy > self.minimum_horizontal_accuracy.into() {
                false
            } else {
                let current_position: Point = user_location.into();
                let current_step_linestring = current_step.get_linestring();

                deviation_from_line(&current_position, &current_step_linestring)
                    .map(|deviation| deviation > self.distance.into())
                    .unwrap_or(false)
            };

        StepAdvanceResult {
            should_advance,
            next_iteration: Arc::new(DistanceFromStep {
                minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
                distance: self.distance,
            }),
        }
    }
}

impl StepAdvanceConditionJsConvertible for DistanceFromStep {
    #[cfg(feature = "wasm-bindgen")]
    fn to_js(&self) -> JsStepAdvanceCondition {
        JsStepAdvanceCondition::DistanceFromStep {
            distance: self.distance,
            minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
        }
    }
}

// MARK: Operator Conditions

/// Advance if any of the conditions are met (OR).
///
/// This is ideal for short circuit type advance conditions.
///
/// E.g. you may have:
/// 1. A short circuit detecting if the user has exceeded a large distance from the current step.
/// 2. A default advance behavior.
#[derive(Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
pub struct OrAdvanceConditions {
    pub conditions: Vec<Arc<dyn StepAdvanceCondition>>,
}

impl StepAdvanceCondition for OrAdvanceConditions {
    fn should_advance_step(
        &self,
        user_location: UserLocation,
        current_step: RouteStep,
        next_step: Option<RouteStep>,
    ) -> StepAdvanceResult {
        let should_advance = self.conditions.iter().any(|c| {
            c.should_advance_step(user_location, current_step.clone(), next_step.clone())
                .should_advance
        });

        StepAdvanceResult {
            should_advance,
            next_iteration: Arc::new(OrAdvanceConditions {
                conditions: self.conditions.clone(),
            }),
        }
    }
}

impl StepAdvanceConditionJsConvertible for OrAdvanceConditions {
    #[cfg(feature = "wasm-bindgen")]
    fn to_js(&self) -> JsStepAdvanceCondition {
        JsStepAdvanceCondition::OrAdvanceConditions {
            conditions: self.conditions.iter().map(|c| c.to_js()).collect(),
        }
    }
}

/// Advance if all of the conditions are met (AND).
#[derive(Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
pub struct AndAdvanceConditions {
    pub conditions: Vec<Arc<dyn StepAdvanceCondition>>,
}

impl StepAdvanceCondition for AndAdvanceConditions {
    fn should_advance_step(
        &self,
        user_location: UserLocation,
        current_step: RouteStep,
        next_step: Option<RouteStep>,
    ) -> StepAdvanceResult {
        let should_advance = self.conditions.iter().all(|c| {
            c.should_advance_step(user_location, current_step.clone(), next_step.clone())
                .should_advance
        });

        StepAdvanceResult {
            should_advance,
            next_iteration: Arc::new(AndAdvanceConditions {
                conditions: self.conditions.clone(),
            }),
        }
    }
}


impl StepAdvanceConditionJsConvertible for AndAdvanceConditions {
    #[cfg(feature = "wasm-bindgen")]
    fn to_js(&self) -> JsStepAdvanceCondition {
        JsStepAdvanceCondition::AndAdvanceConditions {
            conditions: self.conditions.iter().map(|c| c.to_js()).collect(),
        }
    }
}

#[derive(Debug, Copy, Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
pub struct DistanceEntryAndExitCondition {
    /// The minimum required horizontal accuracy of the user location, in meters.
    /// Values larger than this cannot ever trigger a step advance.
    pub minimum_horizontal_accuracy: u16,
    /// Mark the arrival at the end of the step once the user is within this distance.
    pub distance_to_end_of_step: u16,
    /// Advance after the user has left the end of the step by
    /// this distance.
    pub distance_after_end_step: u16,
    /// This becomes true when the user is within range of the end of the step.
    /// Because this step condition is stateful, it must first upgrade this to true,
    /// and then check if the user exited the step by the threshold distance.
    pub has_reached_end_of_current_step: bool,
}

impl DistanceEntryAndExitCondition {
    pub fn new(
        minimum_horizontal_accuracy: u16,
        distance_to_end_of_step: u16,
        distance_after_end_step: u16,
    ) -> Self {
        Self {
            minimum_horizontal_accuracy,
            distance_to_end_of_step,
            distance_after_end_step,
            has_reached_end_of_current_step: false,
        }
    }
}

impl Default for DistanceEntryAndExitCondition {
    fn default() -> Self {
        Self {
            minimum_horizontal_accuracy: 25,
            distance_to_end_of_step: 20,
            distance_after_end_step: 10,
            has_reached_end_of_current_step: false,
        }
    }
}

impl StepAdvanceCondition for DistanceEntryAndExitCondition {
    #[allow(unused_variables)]
    fn should_advance_step(
        &self,
        user_location: UserLocation,
        current_step: RouteStep,
        next_step: Option<RouteStep>,
    ) -> StepAdvanceResult {
        if self.has_reached_end_of_current_step {
            let distance_from_end = DistanceFromStep {
                minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
                distance: self.distance_after_end_step,
            };

            let should_advance = distance_from_end
                .should_advance_step(user_location, current_step, next_step)
                .should_advance;

            if should_advance {
                StepAdvanceResult {
                    should_advance: true,
                    next_iteration: Arc::new(DistanceEntryAndExitCondition::new(
                        self.minimum_horizontal_accuracy,
                        self.distance_to_end_of_step,
                        self.distance_after_end_step,
                    )),
                }
            } else {
                // The condition was not advanced. So we return a fresh iteration
                // where has_reached_end_of_current_step is still true to re-trigger this part 2 logic.
                StepAdvanceResult {
                    should_advance: false,
                    next_iteration: Arc::new(DistanceEntryAndExitCondition {
                        minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
                        distance_to_end_of_step: self.distance_to_end_of_step,
                        distance_after_end_step: self.distance_after_end_step,
                        has_reached_end_of_current_step: true,
                    }),
                }
            }
        } else {
            let distance_to_end = DistanceToEndOfStep {
                minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
                distance: self.distance_to_end_of_step,
            };

            // Use the distance to end to determine if has_reached_end_of_current_step
            let next_iteration = DistanceEntryAndExitCondition {
                minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
                distance_to_end_of_step: self.distance_to_end_of_step,
                distance_after_end_step: self.distance_after_end_step,
                has_reached_end_of_current_step: distance_to_end
                    .should_advance_step(user_location, current_step, next_step)
                    .should_advance,
            };

            StepAdvanceResult {
                should_advance: false,
                next_iteration: Arc::new(next_iteration),
            }
        }
    }
}

impl StepAdvanceConditionJsConvertible for DistanceEntryAndExitCondition {
    #[cfg(feature = "wasm-bindgen")]
    fn to_js(&self) -> JsStepAdvanceCondition {
        JsStepAdvanceCondition::DistanceEntryExit {
            minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
            distance_to_end_of_step: self.distance_to_end_of_step,
            distance_after_end_step: self.distance_after_end_step,
            has_reached_end_of_current_step: self.has_reached_end_of_current_step,
        }
    }
}

#[cfg(test)]
mod tests {
    use crate::models::GeographicCoordinate;
    use std::time::SystemTime;

    use super::*;

    fn user_location(lat: f64, lng: f64, horizontal_accuracy: f64) -> UserLocation {
        UserLocation {
            coordinates: GeographicCoordinate { lat, lng },
            horizontal_accuracy,
            course_over_ground: None,
            timestamp: SystemTime::now(),
            speed: None,
        }
    }

    #[test]
    fn test_manual_step_advance() {
        // Create a straight line route step
        let route_step =
            crate::navigation_controller::test_helpers::gen_route_step_with_coords(vec![
                (0.0, 0.0),   // Origin
                (0.001, 0.0), // 111 meters east at the equator
            ]);

        // Create a location that's far from the end of the step
        // This point is near the beginning of the step, not the end
        let user_location = user_location(0.0, 0.0001, 5.0);

        let condition = ManualStepAdvance;

        // Test the condition - we should NOT advance since we're far from the end
        let result = condition.should_advance_step(user_location, route_step, None);

        // We should never advance to the next step in manual mode,
        // so the list should always be empty.
        assert!(
            !result.should_advance,
            "Should not advance with the manual condition"
        );
    }

    #[test]
    fn test_distance_to_end_of_step_doesnt_advance() {
        // Create a straight line route step
        let route_step =
            crate::navigation_controller::test_helpers::gen_route_step_with_coords(vec![
                (0.0, 0.0),   // Origin
                (0.001, 0.0), // 111 meters east at the equator
            ]);

        // Create a location that's far from the end of the step
        // This point is near the beginning of the step, not the end
        let user_location = user_location(0.0, 0.0001, 5.0);

        // Set up the condition with a distance threshold of 20 meters
        let condition = DistanceToEndOfStep {
            minimum_horizontal_accuracy: 10,
            distance: 20, // Must be within 20 meters of the end to advance
        };

        // Test the condition - we should NOT advance since we're far from the end
        let result = condition.should_advance_step(user_location, route_step, None);

        assert!(
            !result.should_advance,
            "Should not advance when far from the end of the step"
        );
    }

    #[test]
    fn test_distance_to_end_of_step_advance() {
        // Create a straight line route step
        let route_step =
            crate::navigation_controller::test_helpers::gen_route_step_with_coords(vec![
                (0.0, 0.0),   // Origin
                (0.001, 0.0), // 111 meters east at the equator
            ]);

        // Create a location that's very close to the end point of the step
        let user_location = user_location(0.0, 0.00099, 5.0); // Almost at the end point

        // Set up the condition with a distance threshold of 20 meters
        let condition = DistanceToEndOfStep {
            minimum_horizontal_accuracy: 10,
            distance: 20, // Must be within 20 meters of the end to advance
        };

        // Test the condition - we SHOULD advance since we're close to the end
        let result = condition.should_advance_step(user_location, route_step, None);

        assert!(
            result.should_advance,
            "Should advance when close to the end of the step"
        );
    }

    #[test]
    fn test_distance_from_step_doesnt_advance() {
        // Create a straight line route step running east to west
        let route_step =
            crate::navigation_controller::test_helpers::gen_route_step_with_coords(vec![
                (0.0, 0.0),   // Origin
                (0.001, 0.0), // 111 meters east at the equator
            ]);

        // Create a location that's on the line (so deviation = 0)
        let user_location = user_location(0.0, 0.0005, 5.0); // Point directly on the line

        // Set up the condition with a minimum deviation of 100 meters to advance
        let condition = DistanceFromStep {
            minimum_horizontal_accuracy: 10,
            distance: 100, // Must be at least 100 meters from route to advance
        };

        // Test the condition - we should NOT advance since we're right on the route
        let result = condition.should_advance_step(user_location, route_step, None);

        assert!(
            !result.should_advance,
            "Should not advance when on the route"
        );
    }

    #[test]
    fn test_distance_from_step_advance() {
        // Create a straight line route step running east to west
        let route_step =
            crate::navigation_controller::test_helpers::gen_route_step_with_coords(vec![
                (0.0, 0.0),   // Origin
                (0.001, 0.0), // 111 meters east at the equator
            ]);

        // Create a location that's far from the route (500+ meters north)
        // At the equator, 0.005° latitude is approximately 555 meters
        let user_location = user_location(0.005, 0.0005, 5.0);

        // Set up the condition with a minimum deviation of 100 meters to advance
        let condition = DistanceFromStep {
            minimum_horizontal_accuracy: 10,
            distance: 100, // Must be at least 100 meters from route to advance
        };

        // Test the condition - we SHOULD advance since we're far from the route
        let result = condition.should_advance_step(user_location, route_step, None);

        assert!(
            result.should_advance,
            "Should advance when far from the route"
        );
    }

    // Combination Rules

    #[test]
    fn test_or_condition_doesnt_advance() {
        // Create a straight line route step
        let route_step =
            crate::navigation_controller::test_helpers::gen_route_step_with_coords(vec![
                (0.0, 0.0),   // Origin
                (0.001, 0.0), // 111 meters east at the equator
            ]);

        // Create a location for testing
        let user_location = user_location(0.0, 0.0001, 5.0);

        // Create two false conditions - both manual step advance
        let manual_condition1 = ManualStepAdvance;
        let manual_condition2 = ManualStepAdvance;

        // Create an OR condition - should only advance when at least one condition is true
        let or_condition = OrAdvanceConditions {
            conditions: vec![Arc::new(manual_condition1), Arc::new(manual_condition2)],
        };

        // Test the condition - we should NOT advance since both conditions are false
        let result = or_condition.should_advance_step(user_location, route_step, None);

        assert!(
            !result.should_advance,
            "Should not advance when all OR conditions are false"
        );
    }

    #[test]
    fn test_or_condition_advance() {
        // Create a straight line route step
        let route_step =
            crate::navigation_controller::test_helpers::gen_route_step_with_coords(vec![
                (0.0, 0.0),   // Origin
                (0.001, 0.0), // 111 meters east at the equator
            ]);

        // Create a location that's very close to the end point of the step
        let user_location = user_location(0.0, 0.00099, 5.0); // Almost at the end point

        // Create a false condition
        let manual_condition = ManualStepAdvance;

        // Create a true condition - distance to end of step
        let distance_condition = DistanceToEndOfStep {
            minimum_horizontal_accuracy: 10,
            distance: 20, // Must be within 20 meters of the end to advance
        };

        // Create an OR condition - should advance when any condition is true
        let or_condition = OrAdvanceConditions {
            conditions: vec![Arc::new(manual_condition), Arc::new(distance_condition)],
        };

        // Test the condition - we SHOULD advance since one condition is true
        let result = or_condition.should_advance_step(user_location, route_step, None);

        assert!(
            result.should_advance,
            "Should advance when at least one OR condition is true"
        );
    }

    #[test]
    fn test_and_condition_doesnt_advance() {
        // Create a straight line route step
        let route_step =
            crate::navigation_controller::test_helpers::gen_route_step_with_coords(vec![
                (0.0, 0.0),   // Origin
                (0.001, 0.0), // 111 meters east at the equator
            ]);

        // Create a location that's very close to the end point of the step
        let user_location = user_location(0.0, 0.00099, 5.0); // Almost at the end point

        // Create a false condition
        let manual_condition = ManualStepAdvance;

        // Create a true condition - distance to end of step
        let distance_condition = DistanceToEndOfStep {
            minimum_horizontal_accuracy: 10,
            distance: 20, // Must be within 20 meters of the end to advance
        };

        // Create an AND condition - should only advance when all conditions are true
        let and_condition = AndAdvanceConditions {
            conditions: vec![
                Arc::new(manual_condition),   // This will always be false
                Arc::new(distance_condition), // This will be true
            ],
        };

        // Test the condition - we should NOT advance since one condition is false
        let result = and_condition.should_advance_step(user_location, route_step, None);

        assert!(
            !result.should_advance,
            "Should not advance when at least one AND condition is false"
        );
    }

    #[test]
    fn test_and_condition_advance() {
        // Create a straight line route step
        let route_step =
            crate::navigation_controller::test_helpers::gen_route_step_with_coords(vec![
                (0.0, 0.0),   // Origin
                (0.001, 0.0), // 111 meters east at the equator
            ]);

        // Create a location that's very close to the end point of the step
        let user_location = user_location(0.0, 0.00099, 5.0); // Almost at the end point

        // Create two true conditions - both distance to end of step but with different thresholds
        let distance_condition1 = DistanceToEndOfStep {
            minimum_horizontal_accuracy: 10,
            distance: 30, // Must be within 30 meters of the end to advance
        };

        let distance_condition2 = DistanceToEndOfStep {
            minimum_horizontal_accuracy: 10,
            distance: 20, // Must be within 20 meters of the end to advance
        };

        // Create an AND condition - should only advance when all conditions are true
        let and_condition = AndAdvanceConditions {
            conditions: vec![Arc::new(distance_condition1), Arc::new(distance_condition2)],
        };

        // Test the condition - we SHOULD advance since both conditions are true
        let result = and_condition.should_advance_step(user_location, route_step, None);

        assert!(
            result.should_advance,
            "Should advance when all AND conditions are true"
        );
    }

    // Stateful Conditions

    #[test]
    fn test_entry_and_exit_condition_doesnt_advance() {
        // Create a straight line route step
        let route_step =
            crate::navigation_controller::test_helpers::gen_route_step_with_coords(vec![
                (0.0, 0.0),   // Origin
                (0.001, 0.0), // 111 meters east at the equator
            ]);

        // Create a condition that requires proximity to end followed by distance from step
        let condition = DistanceEntryAndExitCondition::new(
            10, // minimum horizontal accuracy
            20, // distance to end of step (20 meters)
            5,  // distance after end step (5 meters)
        );

        // First update: User is close to the end of the step
        let user_location_near_end = user_location(0.0, 0.00099, 5.0); // Almost at the end point

        // Should not advance yet, but should update internal state
        let result1 =
            condition.should_advance_step(user_location_near_end, route_step.clone(), None);

        assert!(
            !result1.should_advance,
            "Should not advance on first update even when near end of step"
        );

        // Second update: User has moved but is still too close to the route
        // This is only 2 meters from the end point, not the required 5 meters
        let user_location_still_close = user_location(0.00002, 0.001, 5.0);

        // Get the next iteration from the first result
        let next_condition = result1.next_iteration;

        // Should still not advance because we haven't moved far enough away
        let result2 =
            next_condition.should_advance_step(user_location_still_close, route_step, None);

        assert!(
            !result2.should_advance,
            "Should not advance when user hasn't moved far enough from the route"
        );
    }

    #[test]
    fn test_entry_and_exit_condition_advance() {
        // Create a straight line route step
        let route_step =
            crate::navigation_controller::test_helpers::gen_route_step_with_coords(vec![
                (0.0, 0.0),   // Origin
                (0.001, 0.0), // 111 meters east at the equator
            ]);

        // Create a condition that requires proximity to end followed by distance from step
        let condition = DistanceEntryAndExitCondition::new(
            10, // minimum horizontal accuracy
            20, // distance to end of step (20 meters)
            5,  // distance after end step (5 meters)
        );

        // First update: User is close to the end of the step
        let user_location_near_end = user_location(0.0, 0.00099, 5.0); // Almost at the end point

        // Should not advance yet, but should update internal state
        let result1 =
            condition.should_advance_step(user_location_near_end, route_step.clone(), None);

        assert!(
            !result1.should_advance,
            "Should not advance on first update even when near end of step"
        );

        // Get the next iteration with updated internal state
        let next_condition = result1.next_iteration;

        // Second update: User has moved far enough from the route (> 5 meters)
        // ~55 meters north of the route (0.0005 degrees latitude)
        let user_location_far = user_location(0.0005, 0.001, 5.0);

        // Now should advance because we've satisfied both conditions sequentially
        let result2 = next_condition.should_advance_step(user_location_far, route_step, None);

        assert!(
            result2.should_advance,
            "Should advance when user has first reached end of step and then moved away"
        );
    }
}
