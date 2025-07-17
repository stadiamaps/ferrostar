use crate::models::Route;
use crate::navigation_controller::models::{
    NavState, NavigationControllerConfig, NavigationRecordingEvent, NavigationRecordingEventData,
    SerializableNavigationControllerConfig,
};
use chrono::Utc;
use serde::{Deserialize, Serialize};

/// Represents a recorded navigation session with its configuration and events.
///
/// # Fields
///
/// * `version` - The version of Ferrostar that created this recording
/// * `initial_timestamp` - When the navigation session started (in milliseconds)
/// * `config` - Configuration settings used for the navigation session
/// * `initial_route` - The route that was initially assigned for navigation
/// * `events` - A chronological list of all navigation events that occurred
#[derive(Serialize, Deserialize, Clone)]
pub struct NavigationRecording {
    pub version: String,
    pub initial_timestamp: i64,
    pub config: SerializableNavigationControllerConfig,
    pub initial_route: Route,
    pub events: Vec<NavigationRecordingEvent>,
}

/// Custom error type for navigation recording operations.
/// Note: Due to UniFFI limitations, we cannot include the underlying error details.
#[derive(Debug, Serialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Error))]
#[cfg_attr(feature = "std", derive(thiserror::Error))]
pub enum RecordingError {
    #[cfg_attr(feature = "std", error("Error serializing navigation recording."))]
    SerializationError,
    #[cfg_attr(
        feature = "std",
        error("Recording is not allowed for this controller.")
    )]
    RecordingNotAllowed,
}

/// Functionality for the navigation controller that is not exported.
impl NavigationRecording {
    /// Creates a new navigation recorder with route configuration and initial state.
    pub fn new(config: NavigationControllerConfig, initial_route: Route) -> Self {
        Self {
            version: env!("CARGO_PKG_VERSION").to_string(),
            initial_timestamp: Utc::now().timestamp_millis(),
            config: SerializableNavigationControllerConfig::from(config),
            initial_route,
            events: Vec::new(),
        }
    }

    /// Serializes the navigation recording to a JSON string.
    ///
    /// # Returns
    ///
    /// - `Ok(String)` - A JSON string representation of the navigation recording
    /// - `Err(RecordingError)` - If there was an error during JSON serialization
    pub fn to_json(&self, events: Vec<NavigationRecordingEvent>) -> Result<String, RecordingError> {
        let mut recording = self.clone();
        recording.events = events;
        serde_json::to_string(&recording).map_err(|_| RecordingError::SerializationError)
    }

    /// Records a [`NavState`] update event.
    ///
    /// # Parameters
    ///
    /// * `old_state` - The previous navigation state that contains recording events
    /// * `new_state` - The updated navigation state to be recorded
    ///
    /// # Returns
    ///
    /// A vector of [`NavigationRecordingEvent`] containing the state update event if `old_state` contains recording events,
    /// otherwise returns an empty vector
    pub fn record_nav_state_update(
        &self,
        old_state: NavState,
        new_state: NavState,
    ) -> Vec<NavigationRecordingEvent> {
        match old_state.recording_events {
            Some(old_events) => Self::add_event(
                old_events,
                NavigationRecordingEventData::NavStateUpdate {
                    nav_state: new_state.into(),
                },
            ),
            None => Vec::new(),
        }
    }

    /// TODO: Actually implement this
    pub fn record_route_update(
        &self,
        old_state: NavState,
        new_route: Route,
    ) -> Vec<NavigationRecordingEvent> {
        match old_state.recording_events {
            Some(old_events) => Self::add_event(
                old_events,
                NavigationRecordingEventData::RouteUpdate { route: new_route },
            ),
            None => Vec::new(),
        }
    }

    /// TODO: Actually implement this
    pub fn record_error(
        &self,
        old_state: NavState,
        error_message: String,
    ) -> Vec<NavigationRecordingEvent> {
        match old_state.recording_events {
            Some(old_events) => Self::add_event(
                old_events,
                NavigationRecordingEventData::Error { error_message },
            ),
            None => Vec::new(),
        }
    }

    /// Helper function to add an event.
    fn add_event(
        mut old_events: Vec<NavigationRecordingEvent>,
        new_event_data: NavigationRecordingEventData,
    ) -> Vec<NavigationRecordingEvent> {
        old_events.push(NavigationRecordingEvent {
            timestamp: Utc::now().timestamp_millis(),
            event_data: new_event_data,
        });
        old_events
    }
}
