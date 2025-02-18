use core::any::Any;

use models::StepAdvanceMode;

use crate::models::UserLocation;

use super::models::NavigatingTripState;

pub mod models;

/// When implementing custom step advance logic, this trait allows you to define
/// whether the condition should advance to the next condition, the next step or not.
pub trait StepAdvanceCondition: Any + Default + Sync + Send {
    fn should_advance_step(
        &mut self,
        user_location: &UserLocation,
        trip_state: &NavigatingTripState,
    ) -> bool;

    fn clone_box(&self) -> Box<Self>;
}

pub struct StepAdvanceFactory;

impl StepAdvanceFactory {
    pub fn create(mode: StepAdvanceMode) -> Box<dyn StepAdvanceCondition> {
        match mode {

        }
    }
}

#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
pub struct StepAdvanceHandler {
    condition: Box<dyn StepAdvanceCondition>,
}

impl stepAdvanceHandler {
    pub fn new(mode: StepAdvanceMode) -> Self {
        Self {
            condition: ,
        }
    }

    pub fn check_condition(&mut self) -> bool {
        self.condition.is_met()
    }
}

// /// Advance if any of the conditions are met.
// /// This can be used for short circuit type advance conditions.
// ///
// /// E.g. you may have:
// /// 1. A short circuit detecting if the user has exceeded a large distance from the current step.
// /// 2. A default advance behavior.
// // #[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
// #[derive(Default)]
// struct OrAdvanceConditions {
//     conditions: Vec<Box<dyn StepAdvanceCondition>>,
// }

// impl StepAdvanceCondition for OrAdvanceConditions {
//     // fn should_advance_step(&self, user_location: UserLocation, current_step: RouteStep) -> bool {
//     //     self.conditions
//     //         .iter()
//     //         .any(|c| c.should_advance_step(user_location, current_step))
//     // }
// }

// #[derive(Default)]
// struct AndAdvanceConditions {
//     conditions: Vec<Box<dyn StepAdvanceCondition>>,
// }

// impl StepAdvanceCondition for AndAdvanceConditions {
//     fn should_advance_step(
//         &mut self,
//         user_location: &UserLocation,
//         trip_state: &NavigatingTripState,
//     ) -> bool {
//         self.conditions
//             .iter()
//             .all(|c| c.should_advance_step(user_location, trip_state))
//     }

//     // fn should_advance_step(
//     //     &mut self,
//     //     user_location: &UserLocation,
//     //     current_step: &RouteStep,
//     // ) -> bool {
//     //     self.conditions
//     //         .iter()
//     //         .all(|c| c.should_advance_step(user_location, current_step))
//     // }
// }
