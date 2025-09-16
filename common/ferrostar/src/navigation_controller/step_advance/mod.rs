//! Step advance condition traits and implementations.
use crate::{
    models::{RouteStep, UserLocation},
    navigation_controller::step_advance::conditions::{
        AndAdvanceConditions, DistanceEntryAndExitCondition, DistanceFromStepCondition,
        DistanceToEndOfStepCondition, ManualStepCondition, OrAdvanceConditions,
    },
};
use serde::{Deserialize, Serialize};
use std::sync::Arc;

#[cfg(feature = "wasm-bindgen")]
use tsify::Tsify;

pub mod conditions;

/// The step advance result is produced on every iteration of the navigation state machine and
/// used by the navigation to build a new [`NavState`](super::NavState) instance for that update.
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
pub struct StepAdvanceResult {
    /// The step should be advanced.
    should_advance: bool,
    /// The next iteration of the step advance condition.
    ///
    /// This is what the navigation controller passes to the next instance of [`NavState`](super::NavState) on the completion of
    /// an update (e.g. a user location update). Use the helper methods on [`StepAdvanceResult`] to ensure proper state handling:
    ///
    /// - [`StepAdvanceResult::next()`] - Unified method that takes a boolean and handles state appropriately
    /// - [`StepAdvanceResult::advance()`] - Creates an advancing result with automatically reset state
    /// - [`StepAdvanceResult::no_advance()`] - Creates a non-advancing result preserving current state
    ///
    /// The trait method [`StepAdvanceCondition::new_instance()`] ensures that composite conditions
    /// (Or/And) properly reset all nested conditions when any condition triggers advancement.
    ///
    /// **CRITICAL**: When advancing, this must be a clean/reset state to prevent state leakage between steps.
    pub next_iteration: Arc<dyn StepAdvanceCondition>,
}

impl StepAdvanceResult {
    /// Whether the step should advance to the next step.
    ///
    /// This is used by the navigation controller to determine whether to process the step advance.
    pub fn should_advance(&self) -> bool {
        self.should_advance
    }

    /// Creates a step advance result that does not advance to the next step.
    /// Uses the provided next_iteration as-is for preserving stateful progress.
    ///
    /// Note: it's up to the caller to determine whether next_interation should reset.
    pub fn continue_with(next_iteration: Arc<dyn StepAdvanceCondition>) -> Self {
        Self {
            should_advance: false,
            next_iteration,
        }
    }

    /// Creates a step advance result that advances to the next step.
    /// Automatically creates a new instance of the condition to ensure proper state isolation.
    pub fn advance_to_new_instance(condition: &dyn StepAdvanceCondition) -> Self {
        Self {
            should_advance: true,
            next_iteration: condition.new_instance(),
        }
    }
}

/// A trait for converting a step advance condition into a JavaScript object for Web/WASM.
pub trait StepAdvanceConditionSerializable {
    fn to_js(&self) -> SerializableStepAdvanceCondition;
}

/// When implementing custom step advance logic, this trait allows you to define
/// whether the condition should advance to the next condition, the next step or not.
///
/// At the moment, these must be implemented in Rust.
#[cfg_attr(feature = "uniffi", uniffi::export)]
pub trait StepAdvanceCondition: StepAdvanceConditionSerializable + Sync + Send {
    // NOTE: This cannot be exported `with_foreign` because of uniffi's Arc implementation.
    // It will cause a stack overflow when with_foreign is used at some point in the trip.

    /// This callback method is used by a step advance condition to receive step updates.
    /// The step advance condition can choose based on its outcome and internal state
    /// whether to advance to the next step or not.
    fn should_advance_step(
        &self,
        user_location: UserLocation,
        current_step: RouteStep,
        next_step: Option<RouteStep>,
    ) -> StepAdvanceResult;

    /// Creates a clean instance of this condition with the same configuration but reset state.
    /// This is used by composite conditions (Or/And) to ensure proper state isolation
    /// when any condition triggers advancement.
    ///
    /// **Implementation Requirements:**
    /// - **Stateless conditions**: Return a copy/clone of self
    /// - **Stateful conditions**: Return a new instance with initial state but preserve configuration parameters
    /// - **Composite conditions**: Recursively create fresh instances of all nested conditions
    ///
    /// This method prevents the state leakage bugs that can cause rapid step advancement
    /// and jumping behavior in navigation.
    fn new_instance(&self) -> Arc<dyn StepAdvanceCondition>;
}

#[derive(Serialize, Deserialize, Clone, Debug)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Enum))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(from_wasm_abi))]
pub enum SerializableStepAdvanceCondition {
    Manual,
    #[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
    DistanceToEndOfStep {
        distance: u16,
        minimum_horizontal_accuracy: u16,
    },
    #[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
    DistanceFromStep {
        distance: u16,
        minimum_horizontal_accuracy: u16,
    },
    #[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
    DistanceEntryExit {
        distance_to_end_of_step: u16,
        distance_after_end_step: u16,
        minimum_horizontal_accuracy: u16,
        has_reached_end_of_current_step: bool,
    },
    #[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
    OrAdvanceConditions {
        conditions: Vec<SerializableStepAdvanceCondition>,
    },
    #[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
    AndAdvanceConditions {
        conditions: Vec<SerializableStepAdvanceCondition>,
    },
}

impl From<SerializableStepAdvanceCondition> for Arc<dyn StepAdvanceCondition> {
    fn from(condition: SerializableStepAdvanceCondition) -> Arc<dyn StepAdvanceCondition> {
        match condition {
            SerializableStepAdvanceCondition::Manual => Arc::new(ManualStepCondition),
            SerializableStepAdvanceCondition::DistanceToEndOfStep {
                distance,
                minimum_horizontal_accuracy,
            } => Arc::new(DistanceToEndOfStepCondition {
                distance,
                minimum_horizontal_accuracy,
            }),
            SerializableStepAdvanceCondition::DistanceFromStep {
                distance,
                minimum_horizontal_accuracy,
            } => Arc::new(DistanceToEndOfStepCondition {
                distance,
                minimum_horizontal_accuracy,
            }),
            SerializableStepAdvanceCondition::DistanceEntryExit {
                minimum_horizontal_accuracy,
                distance_to_end_of_step,
                distance_after_end_step,
                has_reached_end_of_current_step,
            } => Arc::new(DistanceEntryAndExitCondition {
                minimum_horizontal_accuracy,
                distance_to_end_of_step,
                distance_after_end_of_step: distance_after_end_step,
                has_reached_end_of_current_step,
            }),
            SerializableStepAdvanceCondition::OrAdvanceConditions { conditions } => {
                Arc::new(OrAdvanceConditions {
                    conditions: conditions.into_iter().map(|c| c.into()).collect(),
                })
            }
            SerializableStepAdvanceCondition::AndAdvanceConditions { conditions } => {
                Arc::new(AndAdvanceConditions {
                    conditions: conditions.into_iter().map(|c| c.into()).collect(),
                })
            }
        }
    }
}

/// Convenience function for creating a [`ManualStepCondition`].
///
/// This never advances to the next step automatically.
/// You must manually advance to the next step programmatically using a FerrostarCore
/// platform wrapper or by calling [`super::Navigator::advance_to_next_step`] manually.
#[cfg(feature = "uniffi")]
#[uniffi::export]
pub fn step_advance_manual() -> Arc<dyn StepAdvanceCondition> {
    Arc::new(ManualStepCondition)
}

/// Convenience function for creating a [`DistanceToEndOfStepCondition`].
///
/// This advances to the next step when the user is within `distance` meters of the last point in the current route step.
/// Does not advance unless the reported location accuracy is `minimum_horizontal_accuracy` meters or better.
#[cfg(feature = "uniffi")]
#[uniffi::export]
pub fn step_advance_distance_to_end_of_step(
    distance: u16,
    minimum_horizontal_accuracy: u16,
) -> Arc<dyn StepAdvanceCondition> {
    Arc::new(DistanceToEndOfStepCondition {
        distance,
        minimum_horizontal_accuracy,
    })
}

/// Convenience function for creating a [`DistanceFromStepCondition`].
///
/// This advances to the next step when the user is at least `distance` meters away _from_ any point on the current route step geometry.
/// Does not advance unless the reported location accuracy is `minimum_horizontal_accuracy` meters or better.
#[cfg(feature = "uniffi")]
#[uniffi::export]
pub fn step_advance_distance_from_step(
    distance: u16,
    minimum_horizontal_accuracy: u16,
) -> Arc<dyn StepAdvanceCondition> {
    Arc::new(DistanceFromStepCondition {
        distance,
        minimum_horizontal_accuracy,
    })
}

/// Convenience function for creating an [`OrAdvanceConditions`].
///
/// This composes multiple conditions together and advances to the next step if ANY of them trigger.
#[cfg(feature = "uniffi")]
#[uniffi::export]
pub fn step_advance_or(
    conditions: Vec<Arc<dyn StepAdvanceCondition>>,
) -> Arc<dyn StepAdvanceCondition> {
    Arc::new(OrAdvanceConditions { conditions })
}

/// Convenience function for creating an [`AndAdvanceConditions`].
///
/// This composes multiple conditions together and advances to the next step if ALL of them trigger.
#[cfg(feature = "uniffi")]
#[uniffi::export]
pub fn step_advance_and(
    conditions: Vec<Arc<dyn StepAdvanceCondition>>,
) -> Arc<dyn StepAdvanceCondition> {
    Arc::new(AndAdvanceConditions { conditions })
}

/// Convenience function for creating a [`DistanceEntryAndExitCondition`].
///
/// Requires the user to first travel within `distance_to_end_of_step` meters of the end of the step,
/// and then travel at least `distance_after_end_of_step` meters away from the step geometry.
/// This ensures the user completes the maneuver before advancing to the next step.
#[cfg(feature = "uniffi")]
#[uniffi::export]
pub fn step_advance_distance_entry_and_exit(
    distance_to_end_of_step: u16,
    distance_after_end_of_step: u16,
    minimum_horizontal_accuracy: u16,
) -> Arc<dyn StepAdvanceCondition> {
    Arc::new(DistanceEntryAndExitCondition {
        distance_to_end_of_step,
        distance_after_end_of_step,
        minimum_horizontal_accuracy,
        has_reached_end_of_current_step: false,
    })
}
