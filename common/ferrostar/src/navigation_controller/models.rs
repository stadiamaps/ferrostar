//! State and configuration data models.

#[cfg(feature = "wasm-bindgen")]
use super::step_advance::JsStepAdvanceCondition;
use crate::algorithms::distance_between_locations;
use crate::deviation_detection::{RouteDeviation, RouteDeviationTracking};
use crate::models::{
    Route, RouteStep, SpokenInstruction, UserLocation, VisualInstruction, Waypoint,
};
#[cfg(feature = "alloc")]
use alloc::vec::Vec;
use chrono::{DateTime, Utc};
#[cfg(any(feature = "wasm-bindgen", test))]
use serde::{Deserialize, Serialize};
use std::sync::Arc;
#[cfg(feature = "wasm-bindgen")]
use tsify::Tsify;

use super::step_advance::conditions::ManualStepCondition;
use super::step_advance::StepAdvanceCondition;

/// The navigation state.
///
/// This is typically created from an initial trip state
/// and conditions for advancing navigation to the next step.
/// Any internal navigation state is packed in here so that
/// the navigation controller can remain functionally pure.
#[derive(Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
pub struct NavState {
    trip_state: TripState,
    // This has to be here because we actually do need to update internal state that changes throughout navigation.
    step_advance_condition: Arc<dyn StepAdvanceCondition>,
    recording_events: Option<Vec<NavigationRecordingEvent>>,
}

impl NavState {
    /// Creates a new navigation state with the provided trip state and step advance condition.
    pub fn new(
        trip_state: TripState,
        step_advance_condition: Arc<dyn StepAdvanceCondition>,
    ) -> Self {
        Self {
            trip_state,
            step_advance_condition,
            recording_events: None,
        }
    }

    /// Creates a new idle navigation state (no trip currently in progress, but still tracking the user's location).
    pub fn idle(user_location: Option<UserLocation>) -> Self {
        Self {
            trip_state: TripState::Idle { user_location },
            step_advance_condition: Arc::new(ManualStepCondition {}), // No op condition.
            recording_events: None,
        }
    }

    /// Creates a navigation state indicating the trip is complete (arrived at the destination but still tracking the user's location).
    ///
    /// The summary is retained as a snapshot (the caller should have this from the last known state).
    pub fn complete(user_location: UserLocation, last_summary: TripSummary) -> Self {
        Self {
            trip_state: TripState::Complete {
                user_location,
                summary: TripSummary {
                    ended_at: Some(Utc::now()),
                    ..last_summary
                },
            },
            step_advance_condition: Arc::new(ManualStepCondition {}), // No op condition.
            recording_events: None,
        }
    }

    #[inline]
    pub fn trip_state(&self) -> TripState {
        self.trip_state.clone()
    }

    #[inline]
    pub fn step_advance_condition(&self) -> Arc<dyn StepAdvanceCondition> {
        self.step_advance_condition.clone()
    }
}

#[cfg(feature = "wasm-bindgen")]
#[derive(Serialize, Deserialize, Tsify)]
#[serde(rename_all = "camelCase")]
#[tsify(into_wasm_abi, from_wasm_abi)]
pub struct JsNavState {
    trip_state: TripState,
    // This has to be here because we actually do need to update internal state that changes throughout navigation.
    step_advance_condition: JsStepAdvanceCondition,
    recording_events: Option<Vec<NavigationRecordingEvent>>,
}

#[cfg(feature = "wasm-bindgen")]
impl From<JsNavState> for NavState {
    fn from(value: JsNavState) -> Self {
        Self {
            trip_state: value.trip_state,
            step_advance_condition: value.step_advance_condition.into(),
            recording_events: value.recording_events,
        }
    }
}

#[cfg(feature = "wasm-bindgen")]
impl From<NavState> for JsNavState {
    fn from(value: NavState) -> Self {
        Self {
            trip_state: value.trip_state,
            step_advance_condition: value.step_advance_condition.to_js(),
            recording_events: value.recording_events,
        }
    }
}

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

/// Information pertaining to the user's full navigation trip. This includes
/// simple stats like total duration and distance.
#[derive(Debug, Clone, PartialEq)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
#[cfg_attr(any(feature = "wasm-bindgen", test), derive(Serialize, Deserialize))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
#[cfg_attr(any(feature = "wasm-bindgen", test), serde(rename_all = "camelCase"))]
#[cfg_attr(feature = "wasm-bindgen", tsify(into_wasm_abi, from_wasm_abi))]
pub struct TripSummary {
    /// The total raw distance traveled in the trip, in meters.
    pub distance_traveled: f64,
    /// The total snapped distance traveled in the trip, in meters.
    pub snapped_distance_traveled: f64,
    /// When the trip was started.
    #[cfg_attr(feature = "wasm-bindgen", tsify(type = "Date"))]
    pub started_at: DateTime<Utc>,
    /// When the trip was completed or canceled.
    #[cfg_attr(feature = "wasm-bindgen", tsify(type = "Date | null"))]
    pub ended_at: Option<DateTime<Utc>>,
}

impl TripSummary {
    pub(crate) fn update(
        &self,
        previous_location: &UserLocation,
        current_location: &UserLocation,
        previous_snapped_location: &UserLocation,
        current_snapped_location: &UserLocation,
    ) -> Self {
        // Calculate distance increment between the user locations.
        let distance_increment = distance_between_locations(previous_location, current_location);
        let snapped_distance_increment =
            distance_between_locations(previous_snapped_location, current_snapped_location);

        TripSummary {
            distance_traveled: self.distance_traveled + distance_increment,
            snapped_distance_traveled: self.snapped_distance_traveled + snapped_distance_increment,
            started_at: self.started_at,
            ended_at: self.ended_at,
        }
    }
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
#[allow(clippy::large_enum_variant)]
pub enum TripState {
    /// The navigation controller is idle and there is no active trip.
    Idle { user_location: Option<UserLocation> },
    #[cfg_attr(feature = "wasm-bindgen", serde(rename_all = "camelCase"))]
    /// The navigation controller is actively navigating a trip.
    Navigating {
        /// The index of the closest coordinate to the user's snapped location.
        ///
        /// This index is relative to the *current* [`RouteStep`]'s geometry.
        current_step_geometry_index: Option<u64>,
        /// The user's raw location.
        ///
        /// This is more useful than the snapped location when the user is off route,
        /// or in special situations like pedestrian navigation.
        user_location: UserLocation,
        /// The user's location as if they were exactly on the route.
        ///
        /// This is derived by snapping the latitude and longitude to the closest point on the route line,
        /// regardless of where they actually are.
        /// This is desirable as it makes the navigation experience better for vehicular navigation,
        /// removing GPS noise as long as the user is deemed to be on the route.
        ///
        /// All other properties from the [`UserLocation`], including speed and course,
        /// are not affected by snapping.
        snapped_user_location: UserLocation,
        /// The ordered list of steps that remain in the trip.
        ///
        /// The step at the front of the list is always the current step.
        /// We currently assume that you cannot move backward to a previous step.
        remaining_steps: Vec<RouteStep>,
        /// Remaining waypoints to visit on the route.
        ///
        /// The waypoint at the front of the list is always the *next* waypoint "goal."
        /// Unlike the current step, there is no value in tracking the "current" waypoint,
        /// as the main use of waypoints is recalculation when the user deviates from the route.
        /// (In most use cases, a route will have only two waypoints, but more complex use cases
        /// may have multiple intervening points that are visited along the route.)
        /// This list is updated as the user advances through the route.
        remaining_waypoints: Vec<Waypoint>,
        /// The trip progress includes information that is useful for showing the
        /// user's progress along the full navigation trip, the route and its components.
        progress: TripProgress,
        /// Information pertaining to the user's full navigation trip. This includes
        /// simple stats like total duration, and distance.
        summary: TripSummary,
        /// The route deviation status: is the user following the route or not?
        deviation: RouteDeviation,
        /// The visual instruction that should be displayed in the user interface.
        visual_instruction: Option<VisualInstruction>,
        /// The most recent spoken instruction that should be synthesized using TTS.
        ///
        /// Note it is the responsibility of the platform layer to ensure that utterances are not synthesized multiple times. This property simply reports the current spoken instruction.
        spoken_instruction: Option<SpokenInstruction>,
        /// Annotation data at the current location.
        /// This is represented as a json formatted byte array to allow for flexible encoding of custom annotations.
        annotation_json: Option<String>,
    },
    /// The navigation controller has reached the end of the trip.
    Complete {
        user_location: UserLocation,
        /// Information pertaining to the user's full navigation trip. This includes
        /// simple stats like total duration, and distance.
        summary: TripSummary,
    },
}

#[allow(clippy::large_enum_variant)]
pub enum StepAdvanceStatus {
    /// Navigation has advanced, and the information on the next step is embedded.
    Advanced { step: RouteStep },
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

/// Controls when a waypoint should be marked as complete.
///
/// While a route may consist of thousands of points, waypoints are special.
/// A simple trip will have only one waypoint: the final destination.
/// A more complex trip may have several intermediate stops.
/// Just as the navigation state keeps track of which steps remain in the route,
/// it also tracks which waypoints are still remaining.
///
/// Tracking waypoints enables Ferrostar to reroute users when they stray off the route line.
/// The waypoint advance mode specifies how the framework decides
/// that a waypoint has been visited (and is removed from the list).
///
/// NOTE: Advancing to the next *step* and advancing to the next *waypoint*
/// are separate processes.
/// This will not normally cause any issues, but keep in mind that
/// manually advancing to the next step does not *necessarily* imply
/// that the waypoint will be marked as complete!
#[derive(Debug, Copy, Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Enum))]
#[cfg_attr(feature = "wasm-bindgen", derive(Deserialize, Tsify))]
#[cfg_attr(feature = "wasm-bindgen", tsify(from_wasm_abi))]
pub enum WaypointAdvanceMode {
    /// Advance when the waypoint is within a certain range of meters from the user's location.
    WaypointWithinRange(f64),
}

#[derive(Clone)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
pub struct NavigationControllerConfig {
    /// Configures when navigation advances to the next waypoint in the route.
    pub waypoint_advance: WaypointAdvanceMode,
    /// Configures when navigation advances to the next step in the route.
    pub step_advance_condition: Arc<dyn StepAdvanceCondition>,
    /// A special advance condition used for the final 2 route steps (last and arrival).
    ///
    /// This exists because several of our step advance conditions require entry and
    /// exit from a step's geometry. The end of the route/arrival doesn't always accommodate
    /// the expected location updates for the core step advance condition.
    pub arrival_step_advance_condition: Arc<dyn StepAdvanceCondition>,
    /// Configures when the user is deemed to be off course.
    ///
    /// NOTE: This is distinct from the action that is taken.
    /// It is only the determination that the user has deviated from the expected route.
    pub route_deviation_tracking: RouteDeviationTracking,
    /// Configures how the heading component of the snapped location is reported in [`TripState`].
    pub snapped_location_course_filtering: CourseFiltering,
}

#[cfg(feature = "wasm-bindgen")]
#[derive(Deserialize, Tsify)]
#[serde(rename_all = "camelCase")]
#[tsify(from_wasm_abi)]
pub struct JsNavigationControllerConfig {
    /// Configures when navigation advances to the next waypoint in the route.
    pub waypoint_advance: WaypointAdvanceMode,
    /// Configures when navigation advances to the next step in the route.
    pub step_advance_condition: JsStepAdvanceCondition,
    /// A special advance condition used for the final 2 route steps (last and arrival).
    ///
    /// This exists because several of our step advance conditions require entry and
    /// exit from a step's geometry. The end of the route/arrival doesn't always accommodate
    /// the expected location updates for the core step advance condition.
    pub arrival_step_advance_condition: JsStepAdvanceCondition,
    /// Configures when the user is deemed to be off course.
    ///
    /// NOTE: This is distinct from the action that is taken.
    /// It is only the determination that the user has deviated from the expected route.
    pub route_deviation_tracking: RouteDeviationTracking,
    /// Configures how the heading component of the snapped location is reported in [`TripState`].
    pub snapped_location_course_filtering: CourseFiltering,
}

#[cfg(feature = "wasm-bindgen")]
impl From<JsNavigationControllerConfig> for NavigationControllerConfig {
    fn from(js_config: JsNavigationControllerConfig) -> Self {
        Self {
            waypoint_advance: js_config.waypoint_advance,
            step_advance_condition: js_config.step_advance_condition.into(),
            arrival_step_advance_condition: js_config.arrival_step_advance_condition.into(),
            route_deviation_tracking: js_config.route_deviation_tracking,
            snapped_location_course_filtering: js_config.snapped_location_course_filtering,
        }
    }
}

#[cfg_attr(feature = "uniffi", derive(uniffi::Record))]
#[derive(Clone, Debug, PartialEq)]
#[cfg_attr(any(feature = "wasm-bindgen", test), derive(Serialize, Deserialize))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
pub struct NavigationRecordingEvent {
    /// The timestamp of the event.
    pub timestamp: i64,
    /// Data associated with the event.
    pub event_data: NavigationRecordingEventData,
}

#[cfg_attr(feature = "uniffi", derive(uniffi::Enum))]
#[derive(Clone, Debug, PartialEq)]
#[cfg_attr(any(feature = "wasm-bindgen", test), derive(Serialize, Deserialize))]
#[cfg_attr(feature = "wasm-bindgen", derive(Tsify))]
pub enum NavigationRecordingEventData {
    LocationUpdate {
        /// Updated user location.
        user_location: UserLocation,
    },
    TripStateUpdate {
        /// Updated trip state.
        trip_state: TripState,
    },
    RouteUpdate {
        /// Updated route steps.
        route: Route,
    },
    Error {
        /// Error message.
        error_message: String,
    },
}
