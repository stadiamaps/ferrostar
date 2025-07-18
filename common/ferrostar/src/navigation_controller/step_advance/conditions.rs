use std::sync::Arc;

use super::{StepAdvanceCondition, StepAdvanceConditionSerializable, StepAdvanceResult};
use crate::{
    algorithms::{deviation_from_line, is_within_threshold_to_end_of_linestring},
    models::{RouteStep, UserLocation},
};
use geo::Point;

#[cfg(test)]
use proptest::prelude::*;

#[cfg(test)]
use crate::test_utils::{arb_coord, make_user_location};

#[cfg(feature = "wasm-bindgen")]
use serde::{Deserialize, Serialize};

use super::SerializableStepAdvanceCondition;
#[cfg(feature = "wasm-bindgen")]
use tsify::Tsify;

/// Never advances to the next step automatically;
/// requires calling [`NavigationController::advance_to_next_step`](super::NavigationController::advance_to_next_step).
///
/// You can use this to implement custom behaviors in external code.
#[derive(Debug, Copy, Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
pub struct ManualStepCondition;

impl StepAdvanceCondition for ManualStepCondition {
    #[allow(unused_variables)]
    fn should_advance_step(
        &self,
        user_location: UserLocation,
        current_step: RouteStep,
        next_step: Option<RouteStep>,
    ) -> StepAdvanceResult {
        StepAdvanceResult {
            should_advance: false,
            next_iteration: Arc::new(ManualStepCondition),
        }
    }
}

impl StepAdvanceConditionSerializable for ManualStepCondition {
    fn to_js(&self) -> SerializableStepAdvanceCondition {
        SerializableStepAdvanceCondition::Manual
    }
}

// MARK: Basic Conditions

/// Automatically advances when the user's location is close enough to the end of the step.
///
/// This results in an eager advance where the user will jump to the next step when the
/// condition is met.
#[derive(Debug, Copy, Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
pub struct DistanceToEndOfStepCondition {
    /// Distance to the last waypoint in the step, measured in meters, at which to advance.
    pub distance: u16,
    /// The minimum required horizontal accuracy of the user location, in meters.
    /// Values larger than this cannot trigger a step advance.
    pub minimum_horizontal_accuracy: u16,
}

impl StepAdvanceCondition for DistanceToEndOfStepCondition {
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
            next_iteration: Arc::new(DistanceToEndOfStepCondition {
                distance: self.distance,
                minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
            }),
        }
    }
}

impl StepAdvanceConditionSerializable for DistanceToEndOfStepCondition {
    fn to_js(&self) -> SerializableStepAdvanceCondition {
        SerializableStepAdvanceCondition::DistanceToEndOfStep {
            distance: self.distance,
            minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
        }
    }
}

/// Requires that the user be at least this far from the current route step.
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
pub struct DistanceFromStepCondition {
    /// The minimum the distance the user must have travelled from the step's polyline.
    pub distance: u16,
    /// The minimum required horizontal accuracy of the user location, in meters.
    /// Values larger than this cannot ever trigger a step advance.
    pub minimum_horizontal_accuracy: u16,
}

impl StepAdvanceCondition for DistanceFromStepCondition {
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
            next_iteration: Arc::new(DistanceFromStepCondition {
                minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
                distance: self.distance,
            }),
        }
    }
}

impl StepAdvanceConditionSerializable for DistanceFromStepCondition {
    fn to_js(&self) -> SerializableStepAdvanceCondition {
        SerializableStepAdvanceCondition::DistanceFromStep {
            distance: self.distance,
            minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
        }
    }
}

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

impl StepAdvanceConditionSerializable for OrAdvanceConditions {
    fn to_js(&self) -> SerializableStepAdvanceCondition {
        SerializableStepAdvanceCondition::OrAdvanceConditions {
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

impl StepAdvanceConditionSerializable for AndAdvanceConditions {
    fn to_js(&self) -> SerializableStepAdvanceCondition {
        SerializableStepAdvanceCondition::AndAdvanceConditions {
            conditions: self.conditions.iter().map(|c| c.to_js()).collect(),
        }
    }
}

/// A stateful condition that requires the user to reach the end of the step then proceed past it to advance.
#[derive(Debug, Copy, Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
pub struct DistanceEntryAndExitCondition {
    /// Mark the arrival at the end of the step once the user is within this distance.
    pub(super) distance_to_end_of_step: u16,
    /// Advance only after the user has left the end of the step by at least this distance.
    ///
    /// This value should be small to avoid the user appearing stuck on the step when using
    /// visible location snapping.
    pub(super) distance_after_end_of_step: u16,
    /// The minimum required horizontal accuracy of the user location, in meters.
    /// Values larger than this cannot ever trigger a step advance.
    pub(super) minimum_horizontal_accuracy: u16,
    /// Internal state for tracking when the user is within `distance_to_end_of_step` meters from the end of the step.
    /// This allows for stateful advance only after entering a reasonable radues of the goal
    /// and then exiting the area by a separate trigger threshold.
    pub(super) has_reached_end_of_current_step: bool,
    // TODO: Do we want a speed multiplier
}

impl Default for DistanceEntryAndExitCondition {
    fn default() -> Self {
        Self {
            distance_to_end_of_step: 20,
            distance_after_end_of_step: 5,
            minimum_horizontal_accuracy: 25,
            has_reached_end_of_current_step: false,
        }
    }
}

#[cfg(test)]
impl DistanceEntryAndExitCondition {
    pub fn exact() -> Self {
        Self {
            distance_to_end_of_step: 0,
            distance_after_end_of_step: 0,
            minimum_horizontal_accuracy: 0,
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
            let distance_from_end = DistanceFromStepCondition {
                minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
                distance: self.distance_after_end_of_step,
            };

            let should_advance = distance_from_end
                .should_advance_step(user_location, current_step, next_step)
                .should_advance;

            if should_advance {
                StepAdvanceResult {
                    should_advance: true,
                    next_iteration: Arc::new(DistanceEntryAndExitCondition {
                        distance_to_end_of_step: self.distance_to_end_of_step,
                        distance_after_end_of_step: self.distance_after_end_of_step,
                        minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
                        has_reached_end_of_current_step: false,
                    }),
                }
            } else {
                // The condition was not advanced. So we return a fresh iteration
                // where has_reached_end_of_current_step is still true to re-trigger this part 2 logic.
                StepAdvanceResult {
                    should_advance: false,
                    next_iteration: Arc::new(DistanceEntryAndExitCondition {
                        distance_to_end_of_step: self.distance_to_end_of_step,
                        distance_after_end_of_step: self.distance_after_end_of_step,
                        minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
                        has_reached_end_of_current_step: true,
                    }),
                }
            }
        } else {
            let distance_to_end = DistanceToEndOfStepCondition {
                minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
                distance: self.distance_to_end_of_step,
            };

            // Use the distance to end to determine if has_reached_end_of_current_step
            let next_iteration = DistanceEntryAndExitCondition {
                minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
                distance_to_end_of_step: self.distance_to_end_of_step,
                distance_after_end_of_step: self.distance_after_end_of_step,
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

impl StepAdvanceConditionSerializable for DistanceEntryAndExitCondition {
    fn to_js(&self) -> SerializableStepAdvanceCondition {
        SerializableStepAdvanceCondition::DistanceEntryExit {
            minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
            distance_to_end_of_step: self.distance_to_end_of_step,
            distance_after_end_step: self.distance_after_end_of_step,
            has_reached_end_of_current_step: self.has_reached_end_of_current_step,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::navigation_controller::test_helpers::gen_route_step_with_coords;
    use crate::test_utils::make_user_location;
    use geo::coord;
    use std::sync::LazyLock;

    static STRAIGHT_LINE_SHORT_ROUTE_STEP: LazyLock<RouteStep> = LazyLock::new(|| {
        gen_route_step_with_coords(vec![
            coord!(x: 0.0, y: 0.0),   // Origin
            coord!(x: 0.001, y: 0.0), // 111 meters east at the equator
        ])
    });

    static LOCATION_NEAR_START_OF_STEP: LazyLock<UserLocation> =
        LazyLock::new(|| make_user_location(coord!(x: 0.0001, y: 0.0), 5.0));
    static LOCATION_NEAR_END_OF_STEP: LazyLock<UserLocation> =
        LazyLock::new(|| make_user_location(coord!(x: 0.00099, y: 0.0), 5.0));

    #[test]
    fn test_manual_step_advance() {
        let condition = ManualStepCondition;

        // Test the condition - we should NOT advance since we're far from the end
        let result = condition.should_advance_step(
            *LOCATION_NEAR_START_OF_STEP,
            STRAIGHT_LINE_SHORT_ROUTE_STEP.clone(),
            None,
        );

        // We should never advance to the next step in manual mode,
        // so the list should always be empty.
        assert!(
            !result.should_advance,
            "Should not advance with the manual condition"
        );
    }

    #[test]
    fn test_distance_to_end_of_step_doesnt_advance() {
        // Set up the condition with a distance threshold of 20 meters
        let condition = DistanceToEndOfStepCondition {
            minimum_horizontal_accuracy: 10,
            distance: 20, // Must be within 20 meters of the end to advance
        };

        // Test the condition - we should NOT advance since we're far from the end
        let result = condition.should_advance_step(
            *LOCATION_NEAR_START_OF_STEP,
            STRAIGHT_LINE_SHORT_ROUTE_STEP.clone(),
            None,
        );

        assert!(
            !result.should_advance,
            "Should not advance when far from the end of the step"
        );
    }

    #[test]
    fn test_distance_to_end_of_step_advance() {
        // Set up the condition with a distance threshold of 20 meters
        let condition = DistanceToEndOfStepCondition {
            minimum_horizontal_accuracy: 10,
            distance: 20, // Must be within 20 meters of the end to advance
        };

        // Test the condition - we SHOULD advance since we're close to the end
        let result = condition.should_advance_step(
            *LOCATION_NEAR_END_OF_STEP,
            STRAIGHT_LINE_SHORT_ROUTE_STEP.clone(),
            None,
        );

        assert!(
            result.should_advance,
            "Should advance when close to the end of the step"
        );
    }

    #[test]
    fn test_distance_from_step_advance() {
        // Create a location that's far from the route (500+ meters north)
        // At the equator, 0.005° latitude is approximately 555 meters
        let user_location = make_user_location(coord!(x: 0.005, y: 0.0005), 5.0);

        // Set up the condition with a minimum deviation of 100 meters to advance
        let condition = DistanceFromStepCondition {
            minimum_horizontal_accuracy: 10,
            distance: 100, // Must be at least 100 meters from route to advance
        };

        // Test the condition - we SHOULD advance since we're far from the route
        let result = condition.should_advance_step(
            user_location,
            STRAIGHT_LINE_SHORT_ROUTE_STEP.clone(),
            None,
        );

        assert!(
            result.should_advance,
            "Should advance when far from the route"
        );
    }

    // Combination Rules

    #[test]
    fn test_or_condition_doesnt_advance() {
        // Create two false conditions - both manual step advance
        let manual_condition1 = ManualStepCondition;
        let manual_condition2 = ManualStepCondition;

        // Create an OR condition - should only advance when at least one condition is true
        let or_condition = OrAdvanceConditions {
            conditions: vec![Arc::new(manual_condition1), Arc::new(manual_condition2)],
        };

        // Test the condition - we should NOT advance since both conditions are false
        let result = or_condition.should_advance_step(
            *LOCATION_NEAR_START_OF_STEP,
            STRAIGHT_LINE_SHORT_ROUTE_STEP.clone(),
            None,
        );

        assert!(
            !result.should_advance,
            "Should not advance when all OR conditions are false"
        );
    }

    #[test]
    fn test_or_condition_advance() {
        // Create a false condition
        let manual_condition = ManualStepCondition;

        // Create a true condition - distance to end of step
        let distance_condition = DistanceToEndOfStepCondition {
            minimum_horizontal_accuracy: 10,
            distance: 20, // Must be within 20 meters of the end to advance
        };

        // Create an OR condition - should advance when any condition is true
        let or_condition = OrAdvanceConditions {
            conditions: vec![Arc::new(manual_condition), Arc::new(distance_condition)],
        };

        // Test the condition - we SHOULD advance since one condition is true
        let result = or_condition.should_advance_step(
            *LOCATION_NEAR_END_OF_STEP,
            STRAIGHT_LINE_SHORT_ROUTE_STEP.clone(),
            None,
        );

        assert!(
            result.should_advance,
            "Should advance when at least one OR condition is true"
        );
    }

    #[test]
    fn test_and_condition_doesnt_advance() {
        // Create a false condition
        let manual_condition = ManualStepCondition;

        // Create a true condition - distance to end of step
        let distance_condition = DistanceToEndOfStepCondition {
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
        let result = and_condition.should_advance_step(
            *LOCATION_NEAR_END_OF_STEP,
            STRAIGHT_LINE_SHORT_ROUTE_STEP.clone(),
            None,
        );

        assert!(
            !result.should_advance,
            "Should not advance when at least one AND condition is false"
        );
    }

    #[test]
    fn test_and_condition_advance() {
        // Create two true conditions - both distance to end of step but with different thresholds
        let distance_condition1 = DistanceToEndOfStepCondition {
            minimum_horizontal_accuracy: 10,
            distance: 30, // Must be within 30 meters of the end to advance
        };

        let distance_condition2 = DistanceToEndOfStepCondition {
            minimum_horizontal_accuracy: 10,
            distance: 20, // Must be within 20 meters of the end to advance
        };

        // Create an AND condition - should only advance when all conditions are true
        let and_condition = AndAdvanceConditions {
            conditions: vec![Arc::new(distance_condition1), Arc::new(distance_condition2)],
        };

        // Test the condition - we SHOULD advance since both conditions are true
        let result = and_condition.should_advance_step(
            *LOCATION_NEAR_END_OF_STEP,
            STRAIGHT_LINE_SHORT_ROUTE_STEP.clone(),
            None,
        );

        assert!(
            result.should_advance,
            "Should advance when all AND conditions are true"
        );
    }

    // Stateful Conditions

    #[test]
    fn test_entry_and_exit_condition_doesnt_advance() {
        // Create a condition that requires proximity to end followed by distance from step
        let condition = DistanceEntryAndExitCondition {
            distance_to_end_of_step: 10,
            distance_after_end_of_step: 20,
            minimum_horizontal_accuracy: 5,
            has_reached_end_of_current_step: false,
        };

        // First update: User is close to the end of the step...
        // Should not advance yet, but should update internal state
        let result1 = condition.should_advance_step(
            *LOCATION_NEAR_END_OF_STEP,
            STRAIGHT_LINE_SHORT_ROUTE_STEP.clone(),
            None,
        );

        assert!(
            !result1.should_advance,
            "Should not advance on first update even when near end of step"
        );

        // Second update: User has moved but is still too close to the route
        // This is only 2 meters from the end point, not the required 5 meters
        let user_location_still_close = make_user_location(coord!(x: 0.001, y: 0.00002), 5.0);

        // Get the next iteration from the first result
        let next_condition = result1.next_iteration;

        // Should still not advance because we haven't moved far enough away
        let result2 = next_condition.should_advance_step(
            user_location_still_close,
            STRAIGHT_LINE_SHORT_ROUTE_STEP.clone(),
            None,
        );

        assert!(
            !result2.should_advance,
            "Should not advance when user hasn't moved far enough from the route"
        );
    }

    #[test]
    fn test_entry_and_exit_condition_advance() {
        // Create a condition that requires proximity to end followed by distance from step
        let condition = DistanceEntryAndExitCondition {
            distance_to_end_of_step: 10,
            distance_after_end_of_step: 20,
            minimum_horizontal_accuracy: 5,
            has_reached_end_of_current_step: false,
        };

        // First update: User is close to the end of the step...
        // Should not advance yet, but should update internal state
        let result1 = condition.should_advance_step(
            *LOCATION_NEAR_END_OF_STEP,
            STRAIGHT_LINE_SHORT_ROUTE_STEP.clone(),
            None,
        );

        assert!(
            !result1.should_advance,
            "Should not advance on first update even when near end of step"
        );

        // Get the next iteration with updated internal state
        let next_condition = result1.next_iteration;

        // Second update: User has moved far enough from the route (> 5 meters)
        // ~55 meters north of the route (0.0005 degrees latitude)
        let user_location_far = make_user_location(coord!(x: 0.001, y: 0.0005), 5.0);

        // Now should advance because we've satisfied both conditions sequentially
        let result2 = next_condition.should_advance_step(
            user_location_far,
            STRAIGHT_LINE_SHORT_ROUTE_STEP.clone(),
            None,
        );

        assert!(
            result2.should_advance,
            "Should advance when user has first reached end of step and then moved away"
        );
    }
}

#[cfg(test)]
proptest! {
    #[test]
    fn manual_step_never_advances(
        c1 in arb_coord(),
        c2 in arb_coord(),
        accuracy: f64
    ) {
        // Create a straight line random route step
        let route_step =
            crate::navigation_controller::test_helpers::gen_route_step_with_coords(vec![c1, c2]);

        // User location exactly at the end of the step
        let user_location = make_user_location(c2, accuracy);

        let condition = ManualStepCondition;

        // Test the condition - we should NOT advance since we're far from the end
        let result = condition.should_advance_step(user_location, route_step, None);

        // We should never advance to the next step in manual mode,
        // so the list should always be empty.
        prop_assert!(
            !result.should_advance,
            "Should not advance with the manual condition"
        );
    }

    #[test]
    fn entry_and_exit_never_advances_on_zero_movement(
        c1 in arb_coord(),
        c2 in arb_coord(),
    ) {
        // Create a straight line random route step
        let route_step =
            crate::navigation_controller::test_helpers::gen_route_step_with_coords(vec![c1, c2]);

        // User location exactly at the end of the step
        let user_location = make_user_location(c2, 5.0);

        // Create a condition that requires proximity to end followed by distance from step
        let condition = DistanceEntryAndExitCondition {
            distance_to_end_of_step: 10,
            distance_after_end_of_step: 20,
            minimum_horizontal_accuracy: 5,
            has_reached_end_of_current_step: false,
        };

        // First update: User is close to the end of the step...
        // Should not advance yet, but should update internal state
        let result1 =
            condition.should_advance_step(user_location, route_step.clone(), None);

        prop_assert!(
            !result1.should_advance,
            "Should not advance on first update even when at the end of the step"
        );

        // Second update: User has not moved

        // Get the next iteration from the first result
        let next_condition = result1.next_iteration;

        // Should still not advance because we haven't moved far enough away
        let result2 =
            next_condition.should_advance_step(user_location, route_step, None);

        prop_assert!(
            !result2.should_advance,
            "Should not advance when user hasn't moved far enough from the route"
        );
    }

    // TODO: handling of accuracy parameter for "always" advance

    // TODO: "or" advance with one condition that's always trivially true and another which is always false

    // TODO: enter+exit with two updates: one exact, and another that exceeds the distance threshold (could generate such a coordinate with polar formulas; probably a crate for that if our existing ones can't do it)

    // TODO: Similar to the above, but with, say, 5 random updates where the user is *always* within the distance threshold, so they never advance to the next step
}
