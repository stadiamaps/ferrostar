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

// MARK: Basic Conditions

/// Automatically advances when the user's location is close enough to the end of the step
#[derive(Debug, Copy, Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
#[cfg_attr(feature = "wasm-bindgen", derive(Deserialize, Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(from_wasm_abi))]
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

/// Allows navigation to advance to the next step as soon as the user
/// comes within this distance (in meters) of the end of the current step.
///
/// This results in *early* advance when the user is near the goal.
pub struct DistanceFromEndOfStep {
    /// The minimum required horizontal accuracy of the user location, in meters.
    /// Values larger than this cannot ever trigger a step advance.
    pub minimum_horizontal_accuracy: u16,
    /// The minimum the distance the user must have travelled from the step's polyline.
    pub distance: u16,
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
/// NOTE! This may be less robust to things like short steps, out and backs and U-turns,
/// where this may eagerly exit a current step before the user has traversed it if the start
/// the step within range of the end.
pub struct MinimumDistanceFromCurrentStepLine {
    /// The minimum required horizontal accuracy of the user location, in meters.
    /// Values larger than this cannot ever trigger a step advance.
    pub minimum_horizontal_accuracy: u16,
    /// The minimum the distance the user must have travelled from the step's polyline.
    pub distance: u16,
}

impl StepAdvanceCondition for MinimumDistanceFromCurrentStepLine {
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
#[derive(Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
#[cfg_attr(feature = "wasm-bindgen", derive(Deserialize, Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(from_wasm_abi))]
pub struct OrAdvanceConditions {
    conditions: Vec<Arc<dyn StepAdvanceCondition>>,
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
#[derive(Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
#[cfg_attr(feature = "wasm-bindgen", derive(Deserialize, Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(from_wasm_abi))]
pub struct AndAdvanceConditions {
    conditions: Vec<Arc<dyn StepAdvanceCondition>>,
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
pub struct DistanceEntryAndExitCondition {
    /// The minimum required horizontal accuracy of the user location, in meters.
    /// Values larger than this cannot ever trigger a step advance.
    minimum_horizontal_accuracy: u16,
    /// Mark the arrival at the end of the step once the user is within this distance.
    distance_to_end_of_step: u16,
    /// Advance after the user has left the end of the step by
    /// this distance.
    distance_after_end_step: u16,
    /// This becomes true when the user is within range of the end of the step.
    /// Because this step condition is stateful, it must first upgrade this to true,
    /// and then check if the user exited the step by the threshold distance.
    has_reached_end_of_current_step: bool,
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
            minimum_horizontal_accuracy: 0,
            distance_to_end_of_step: 10,
            distance_after_end_step: 5,
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
            let distance_from_end = DistanceFromEndOfStep {
                minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
                distance: self.distance_after_end_step,
            };

            let should_advance = distance_from_end
                .should_advance_step(user_location, current_step, next_step)
                .should_advance;

            StepAdvanceResult {
                should_advance: should_advance.clone(),
                next_iteration: Arc::new(DistanceEntryAndExitCondition::new(
                    self.minimum_horizontal_accuracy,
                    self.distance_to_end_of_step,
                    self.distance_after_end_step,
                )),
            }
        } else {
            let distance_to_end = DistanceToEndOfStep {
                minimum_horizontal_accuracy: self.minimum_horizontal_accuracy,
                distance: self.distance_to_end_of_step,
            };

            let mut next_iteration = self.clone();
            // Use the distance to end to determine if has_reached_end_of_current_step
            next_iteration.has_reached_end_of_current_step = distance_to_end
                .should_advance_step(user_location, current_step, next_step)
                .should_advance;

            StepAdvanceResult {
                should_advance: false,
                next_iteration: Arc::new(next_iteration),
            }
        }
    }
}
