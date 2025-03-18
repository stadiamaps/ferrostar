use crate::models::{RouteStep, UserLocation};
use std::sync::Arc;

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

/// When implementing custom step advance logic, this trait allows you to define
/// whether the condition should advance to the next condition, the next step or not.
#[cfg_attr(feature = "uniffi", uniffi::export(with_foreign))]
pub trait StepAdvanceCondition: Sync + Send {
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
