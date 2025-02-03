//! State and configuration data models.

use crate::deviation_detection::{RouteDeviation, RouteDeviationTracking};
use crate::models::{RouteStep, SpokenInstruction, UserLocation, VisualInstruction, Waypoint};
#[cfg(feature = "alloc")]
use alloc::vec::Vec;
use std::cell::RefCell;
use geo::LineString;
#[cfg(any(feature = "wasm-bindgen", test))]
use serde::{Deserialize, Serialize};
#[cfg(feature = "wasm-bindgen")]
use tsify::Tsify;

/// High-level state describing progress through a route.
#[derive(Debug, Clone, PartialEq)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
#[cfg_attr(any(feature = "wasm-bindgen", test), derive(Serialize, Deserialize))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(any(feature = "wasm-bindgen", test), serde(rename_all = "camelCase"))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub struct TripProgress {
    /// The distance to the next maneuver, in meters.
    pub distance_to_next_maneuver: f64,
    /// The total distance remaining in the trip, in meters.
    ///
    /// This is the sum of the distance remaining in the current step and the distance remaining in all subsequent steps.
    pub distance_remaining: f64,
    /// The total duration remaining in the trip, in seconds.
    pub duration_remaining: f64,
}

/// The state of a navigation session.
///
/// This is produced by [`NavigationController`](super::NavigationController) methods
/// including [`get_initial_state`](super::NavigationController::get_initial_state)
/// and [`update_user_location`](super::NavigationController::update_user_location).
#[derive(Debug, Clone, PartialEq)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Enum))]
#[cfg_attr(any(feature = "wasm-bindgen", test), derive(Serialize, Deserialize))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub enum TripState {
    /// The navigation controller is idle and there is no active trip.
    Idle,
    #[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
    /// The navigation controller is actively navigating a trip.
    /// TODO: Maybe break these fields out into a separate state
    Navigating(NavigatingTripState),
    /// The navigation controller has reached the end of the trip.
    Complete,
}

#[derive(Debug, Clone, PartialEq)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
#[cfg_attr(any(feature = "wasm-bindgen", test), derive(Serialize, Deserialize))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub struct NavigatingTripState {
    // TODO: Probably make these accessor methods
    /// The index of the closest coordinate to the user's snapped location.
    ///
    /// This index is relative to the *current* [`RouteStep`]'s geometry.
    pub current_step_geometry_index: Option<u64>,
    /// A location on the line string that
    pub snapped_user_location: UserLocation,
    /// The ordered list of steps that remain in the trip.
    ///
    /// The step at the front of the list is always the current step.
    /// We currently assume that you cannot move backward to a previous step.
    pub remaining_steps: Vec<RouteStep>,
    /// Remaining waypoints to visit on the route.
    ///
    /// The waypoint at the front of the list is always the *next* waypoint "goal."
    /// Unlike the current step, there is no value in tracking the "current" waypoint,
    /// as the main use of waypoints is recalculation when the user deviates from the route.
    /// (In most use cases, a route will have only two waypoints, but more complex use cases
    /// may have multiple intervening points that are visited along the route.)
    /// This list is updated as the user advances through the route.
    pub remaining_waypoints: Vec<Waypoint>,
    /// The trip progress includes information that is useful for showing the
    /// user's progress along the full navigation trip, the route and its components.
    pub progress: TripProgress,
    /// The route deviation status: is the user following the route or not?
    pub deviation: RouteDeviation,
    /// The visual instruction that should be displayed in the user interface.
    pub visual_instruction: Option<VisualInstruction>,
    /// The most recent spoken instruction that should be synthesized using TTS.
    ///
    /// Note it is the responsibility of the platform layer to ensure that utterances are not synthesized multiple times. This property simply reports the current spoken instruction.
    pub spoken_instruction: Option<SpokenInstruction>,
    /// Annotation data at the current location.
    /// This is represented as a json formatted byte array to allow for flexible encoding of custom annotations.
    pub annotation_json: Option<String>,
    /// The step advance condition(s; this may be a conditional tree effectively).
    ///
    /// This gets replaced with the default value on advancing to a new step.
    pub current_condition: Box<dyn StepAdvanceCondition>,
}

#[allow(clippy::large_enum_variant)]
pub enum StepAdvanceStatus {
    /// Navigation has advanced, and the information on the next step is embedded.
    Advanced {
        step: RouteStep,
        linestring: LineString,
    },
    /// Navigation has reached the end of the route.
    EndOfRoute,
}

/// Controls filtering/post-processing of user course by the [`NavigationController`].
#[derive(Debug, Copy, Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Enum))]
#[cfg_attr(feature = "wasm-bindgen", derive(Deserialize, Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(from_wasm_abi))]
pub enum CourseFiltering {
    /// Snap the user's course to the current step's linestring using the next index in the step's geometry.
    ///
    // TODO: We could make this more flexible by allowing the user to specify number of indices to average, etc.
    SnapToRoute,

    /// Use the raw course as reported by the location provider with no processing.
    Raw,
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

/// When implementing custom step advance logic, this trait allows you to define
/// whether the condition should advance to the next condition, the next step or not.
// TODO: Figure out how to export the trait with UniFFI or introduce a "wrapper" exportable implementation
// #[cfg_attr(feature = "uniffi", uniffi::export(with_foreign))]
pub trait StepAdvanceCondition: Default {
    /// Immediately advance entirely to the next step regardless of following conditions.
    ///
    /// This is a way to short circuit the condition evaluation and advance to the next step immediately.
    fn should_advance_step(&mut self, user_location: &UserLocation, current_step: &NavigatingTripState) -> bool;

    /// Creates a fresh instance free of any internal state modifications.
    ///
    /// Uses [Default::default] internally.
    fn new_instance(&self) -> Self {
        Self::default()
    }
}

#[derive(Default)]
struct EntryAndExitCondition {
    has_entered: bool,
}

impl StepAdvanceCondition for EntryAndExitCondition {
    // FIXME: navigating state
    fn should_advance_step(&mut self, user_location: &UserLocation, current_step: &NavigatingTripState) -> bool {
        // Do some real check her
        if self.has_entered {
            true
        } else {
            self.has_entered = true;
            false
        }
    }
}

#[derive(Default)]
struct OrAdvanceConditions {
    conditions: Vec<Box<dyn StepAdvanceCondition>>,
}

impl StepAdvanceCondition for OrAdvanceConditions {
    fn should_advance_step(&mut self, user_location: &UserLocation, current_step: &RouteStep) -> bool {
        self.conditions
            .iter()
            .any(|c| c.should_advance_step(user_location, current_step))
    }
}

#[derive(Default)]
struct AndAdvanceConditions {
    conditions: Vec<Box<dyn StepAdvanceCondition>>,
}

impl StepAdvanceCondition for AndAdvanceConditions {
    fn should_advance_step(&mut self, user_location: &UserLocation, current_step: &RouteStep) -> bool {
        self.conditions
            .iter()
            .all(|c| c.should_advance_step(user_location, current_step))
    }
}

#[derive(Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
#[cfg_attr(feature = "wasm-bindgen", derive(Deserialize, Tsify))]
#[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
#[cfg_attr(feature = "wasm-bindgen", tsify(from_wasm_abi))]
pub struct NavigationControllerConfig {
    pub step_advance_condition: Box<dyn StepAdvanceCondition>,
    /// Configures when the user is deemed to be off course.
    ///
    /// NOTE: This is distinct from the action that is taken.
    /// It is only the determination that the user has deviated from the expected route.
    pub route_deviation_tracking: RouteDeviationTracking,
    /// Configures how the heading component of the snapped location is reported in [`TripState`].
    pub snapped_location_course_filtering: CourseFiltering,
}
