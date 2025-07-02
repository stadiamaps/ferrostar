use crate::{
    models::{RouteStep, UserLocation},
    navigation_controller::step_advance::conditions::{
        AndAdvanceConditions, DistanceEntryAndExitCondition, DistanceFromStepCondition,
        DistanceToEndOfStepCondition, ManualStepCondition, OrAdvanceConditions,
    },
};

#[cfg(feature = "wasm-bindgen")]
use serde::{Deserialize, Serialize};
use std::sync::Arc;

#[cfg(feature = "wasm-bindgen")]
use tsify::Tsify;

pub mod conditions;

#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
pub struct StepAdvanceResult {
    /// The step should be advanced.
    pub should_advance: bool,
    /// The next iteration of the step advance condition.
    /// This allows us to copy the condition and its current state
    /// to the next user location update/next interaction of the step
    /// advance calculation.
    ///
    /// IMPORTANT! If the condition advances. This **must** be the clean/default state.
    pub next_iteration: Arc<dyn StepAdvanceCondition>,
}

pub trait StepAdvanceConditionJsConvertible {
    #[cfg(feature = "wasm-bindgen")]
    fn to_js(&self) -> JsStepAdvanceCondition;
}

/// When implementing custom step advance logic, this trait allows you to define
/// whether the condition should advance to the next condition, the next step or not.
#[cfg_attr(feature = "uniffi", uniffi::export)]
pub trait StepAdvanceCondition: StepAdvanceConditionJsConvertible + Sync + Send {
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
}

#[cfg(feature = "wasm-bindgen")]
#[derive(Serialize, Deserialize, Clone, Debug, Tsify)]
#[tsify(from_wasm_abi)]
pub enum JsStepAdvanceCondition {
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
        minimum_horizontal_accuracy: u16,
        distance_to_end_of_step: u16,
        distance_after_end_step: u16,
        has_reached_end_of_current_step: bool,
    },
    #[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
    OrAdvanceConditions {
        conditions: Vec<JsStepAdvanceCondition>,
    },
    #[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
    AndAdvanceConditions {
        conditions: Vec<JsStepAdvanceCondition>,
    },
}

#[cfg(feature = "wasm-bindgen")]
impl From<JsStepAdvanceCondition> for Arc<dyn StepAdvanceCondition> {
    fn from(condition: JsStepAdvanceCondition) -> Arc<dyn StepAdvanceCondition> {
        match condition {
            JsStepAdvanceCondition::Manual => Arc::new(ManualStepCondition),
            JsStepAdvanceCondition::DistanceToEndOfStep {
                distance,
                minimum_horizontal_accuracy,
            } => Arc::new(DistanceToEndOfStepCondition {
                distance,
                minimum_horizontal_accuracy,
            }),
            JsStepAdvanceCondition::DistanceFromStep {
                distance,
                minimum_horizontal_accuracy,
            } => Arc::new(DistanceToEndOfStepCondition {
                distance,
                minimum_horizontal_accuracy,
            }),
            JsStepAdvanceCondition::DistanceEntryExit {
                minimum_horizontal_accuracy,
                distance_to_end_of_step,
                distance_after_end_step,
                has_reached_end_of_current_step,
            } => Arc::new(DistanceEntryAndExitCondition {
                minimum_horizontal_accuracy,
                distance_to_end_of_step,
                distance_after_end_step,
                has_reached_end_of_current_step,
            }),
            JsStepAdvanceCondition::OrAdvanceConditions { conditions } => {
                Arc::new(OrAdvanceConditions {
                    conditions: conditions.into_iter().map(|c| c.into()).collect(),
                })
            }
            JsStepAdvanceCondition::AndAdvanceConditions { conditions } => {
                Arc::new(AndAdvanceConditions {
                    conditions: conditions.into_iter().map(|c| c.into()).collect(),
                })
            }
        }
    }
}

#[cfg(feature = "uniffi")]
#[uniffi::export]
pub fn step_advance_manual() -> Arc<dyn StepAdvanceCondition> {
    Arc::new(ManualStepCondition)
}

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

#[cfg(feature = "uniffi")]
#[uniffi::export]
pub fn step_advance_or(
    conditions: Vec<Arc<dyn StepAdvanceCondition>>,
) -> Arc<dyn StepAdvanceCondition> {
    Arc::new(OrAdvanceConditions { conditions })
}

#[cfg(feature = "uniffi")]
#[uniffi::export]
pub fn step_advance_and(
    conditions: Vec<Arc<dyn StepAdvanceCondition>>,
) -> Arc<dyn StepAdvanceCondition> {
    Arc::new(AndAdvanceConditions { conditions })
}

#[cfg(feature = "uniffi")]
#[uniffi::export]
pub fn step_advance_distance_entry_and_exit(
    distance_to_end_of_step: u16,
    distance_after_end_step: u16,
    minimum_horizontal_accuracy: u16,
) -> Arc<dyn StepAdvanceCondition> {
    Arc::new(DistanceEntryAndExitCondition::new(
        distance_to_end_of_step,
        distance_after_end_step,
        minimum_horizontal_accuracy,
    ))
}
