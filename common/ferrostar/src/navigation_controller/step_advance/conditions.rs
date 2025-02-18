use std::sync::Arc;

use crate::{
    algorithms::{deviation_from_line, is_within_threshold_to_end_of_linestring},
    models::{RouteStep, UserLocation},
};
use geo::Point;

use super::{StepAdvanceCondition, StepAdvanceResult};

// MARK: Manual

/// Never advances to the next step automatically;
/// requires calling [`NavigationController::advance_to_next_step`](super::NavigationController::advance_to_next_step).
///
/// You can use this to implement custom behaviors in external code.
#[derive(Debug, Copy, Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
#[cfg_attr(feature = "wasm-bindgen", derive(Deserialize, Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(from_wasm_abi))]
struct ManualStepAdvance;

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

// MARK: Basic Conditions

/// Automatically advances when the user's location is close enough to the end of the step
#[derive(Debug, Copy, Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
#[cfg_attr(feature = "wasm-bindgen", derive(Deserialize, Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(from_wasm_abi))]
struct DistanceToEndOfStep {
    /// Distance to the last waypoint in the step, measured in meters, at which to advance.
    distance: u16,
    /// The minimum required horizontal accuracy of the user location, in meters.
    /// Values larger than this cannot trigger a step advance.
    minimum_horizontal_accuracy: u16,
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

/// Allows navigation to advance to the next step as soon as the user
/// comes within this distance (in meters) of the end of the current step.
///
/// This results in *early* advance when the user is near the goal.
struct DistanceFromEndOfStep {
    /// The minimum required horizontal accuracy of the user location, in meters.
    /// Values larger than this cannot ever trigger a step advance.
    minimum_horizontal_accuracy: u16,
    distance: u16,
}

impl StepAdvanceCondition for DistanceFromEndOfStep {
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
            } else if is_within_threshold_to_end_of_linestring(
                &user_location.into(),
                &current_step.get_linestring(),
                f64::from(self.distance),
            ) {
                true
            } else {
                false
            };

        StepAdvanceResult {
            should_advance,
            next_iteration: Arc::new(DistanceFromEndOfStep {
                minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
                distance: self.distance,
            }),
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
/// Note that this could be theoretically less robust to things like U-turns,
/// but we need a bit more real-world testing to confirm if it's an issue.
struct MinimumDistanceFromCurrentStepLine {
    /// The minimum required horizontal accuracy of the user location, in meters.
    /// Values larger than this cannot ever trigger a step advance.
    minimum_horizontal_accuracy: u16,
    distance: u16,
}

impl StepAdvanceCondition for MinimumDistanceFromCurrentStepLine {
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

                // Short-circuit: do NOT advance if we are within `distance`
                // of the current route step.
                //
                // Historical note: we previously considered checking distance from the
                // end of the current step instead, but this actually failed
                // the self-intersecting route tests, since the step break isn't
                // necessarily near the intersection.
                //
                // The last step is special and this logic does not apply.
                if let Some(next_step) = next_step {
                    // Note this special next_step distance check; otherwise we get stuck at the end!
                    if next_step.distance > f64::from(self.distance)
                        && deviation_from_line(&current_position, &current_step_linestring)
                            .map_or(true, |deviation| deviation <= f64::from(self.distance))
                    {
                        return false;
                    }
                }

                let next_step_linestring = next_step?.get_linestring();

                if let (Some(current_step_closest_point), Some(next_step_closest_point)) = (
                    snap_point_to_line(&current_position, current_step_linestring),
                    snap_point_to_line(&current_position, &next_step_linestring),
                ) {
                    // If the user's distance to the snapped location on the *next* step is <=
                    // the user's distance to the snapped location on the *current* step,
                    // advance to the next step
                    Haversine::distance(current_position, next_step_closest_point)
                        <= Haversine::distance(current_position, current_step_closest_point)
                } else {
                    // The user's location couldn't be mapped to a single point on both the current and next step.
                    // Fall back to the distance to end of step mode, which has some graceful fallbacks.
                    // In real-world use, this should only happen for values which are EXTREMELY close together.
                    DistanceToEndOfStep::should_advance_step(user_location, current_step, next_step)
                }
            };

        StepAdvanceResult {
            should_advance,
            next_iteration: Arc::new(MinimumDistanceFromCurrentStepLine {
                minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
                distance: self.distance,
            }),
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
#[derive(Debug, Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
#[cfg_attr(feature = "wasm-bindgen", derive(Deserialize, Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(from_wasm_abi))]
struct OrAdvanceConditions {
    conditions: Vec<Box<dyn StepAdvanceCondition>>,
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

/// Advance if all of the conditions are met (AND).
#[derive(Debug, Copy, Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
#[cfg_attr(feature = "wasm-bindgen", derive(Deserialize, Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(from_wasm_abi))]
struct AndAdvanceConditions {
    conditions: Vec<Box<dyn StepAdvanceCondition>>,
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

#[derive(Debug, Copy, Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
#[cfg_attr(feature = "wasm-bindgen", derive(Deserialize, Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(from_wasm_abi))]
struct EntryAndExitCondition {
    /// This becomes true when the user is within range of the end of the step.
    /// Because this step condition is stateful, it must first upgrade this to true,
    /// and then check if the user exited the step by the threshold distance.
    has_reached_end_of_current_step: bool,
}

impl Default for EntryAndExitCondition {
    fn default() -> Self {
        EntryAndExitCondition {
            has_reached_end_of_current_step: false,
        }
    }
}

impl StepAdvanceCondition for EntryAndExitCondition {
    #[allow(unused_variables)]
    fn should_advance_step(
        &self,
        user_location: UserLocation,
        current_step: RouteStep,
        next_step: Option<RouteStep>,
    ) -> StepAdvanceResult {
        // Do some real check here
        if self.has_reached_end_of_current_step {
            StepAdvanceResult {
                should_advance: true,
                next_iteration: Arc::new(EntryAndExitCondition::default()),
            }
        } else {
            StepAdvanceResult {
                should_advance: false,
                next_iteration: Arc::new(EntryAndExitCondition {
                    has_reached_end_of_current_step: true,
                }),
            }
        }
    }
}
