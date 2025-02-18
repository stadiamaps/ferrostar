use crate::{models::UserLocation, navigation_controller::models::NavigatingTripState};

use super::StepAdvanceCondition;

struct EntryAndExitCondition {
    has_entered: bool,
}

impl Default for EntryAndExitCondition {
    fn default() -> Self {
        EntryAndExitCondition { has_entered: false }
    }
}

impl StepAdvanceCondition for EntryAndExitCondition {
    #[allow(unused_variables)]
    fn should_advance_step(
        &mut self,
        user_location: &UserLocation,
        trip_state: &NavigatingTripState,
    ) -> bool {
        // Do some real check her
        if self.has_entered {
            true
        } else {
            self.has_entered = true;
            false
        }
    }

    fn clone_box(&self) -> Box<Self> {
        Box::new(EntryAndExitCondition {
            has_entered: self.has_entered,
        })
    }
}

/// The step advance mode describes when the current maneuver has been successfully completed,
/// and we should advance to the next step.
#[derive(Debug, Copy, Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Enum))]
#[cfg_attr(feature = "wasm-bindgen", derive(Deserialize, Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(from_wasm_abi))]
pub enum StepAdvanceMode {
    /// Never advances to the next step automatically;
    /// requires calling [`NavigationController::advance_to_next_step`](super::NavigationController::advance_to_next_step).
    ///
    /// You can use this to implement custom behaviors in external code.
    Manual,
    /// Automatically advances when the user's location is close enough to the end of the step
    #[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
    DistanceToEndOfStep {
        /// Distance to the last waypoint in the step, measured in meters, at which to advance.
        distance: u16,
        /// The minimum required horizontal accuracy of the user location, in meters.
        /// Values larger than this cannot trigger a step advance.
        minimum_horizontal_accuracy: u16,
    },
    /// Automatically advances when the user's distance to the *next* step's linestring  is less
    /// than the distance to the current step's linestring, subject to certain conditions.
    #[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
    RelativeLineStringDistance {
        /// The minimum required horizontal accuracy of the user location, in meters.
        /// Values larger than this cannot ever trigger a step advance.
        minimum_horizontal_accuracy: u16,
        /// Optional extra conditions which refine the step advance logic.
        ///
        /// See the enum variant documentation for details.
        special_advance_conditions: Option<SpecialAdvanceConditions>,
    },
    // TODO: Customizations for this if it works.
    /// The entry and exit method for step advance.
    EntryAndExit,
}

/// Special conditions which alter the normal step advance logic,
#[derive(Debug, Copy, Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Enum))]
#[cfg_attr(feature = "wasm-bindgen", derive(Deserialize, Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(from_wasm_abi))]
pub enum SpecialAdvanceConditions {
    /// Allows navigation to advance to the next step as soon as the user
    /// comes within this distance (in meters) of the end of the current step.
    ///
    /// This results in *early* advance when the user is near the goal.
    AdvanceAtDistanceFromEnd(u16),
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
    MinimumDistanceFromCurrentStepLine(u16),
}
