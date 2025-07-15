use crate::models::Route;
use crate::navigation_controller::models::{
    NavState, NavigationControllerConfig, NavigationRecordingEvent, NavigationRecordingEventData,
    SerializableNavigationControllerConfig, TripState,
};
use chrono::Utc;
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize)]
pub struct NavigationRecording {
    /// Version of Ferrostar that created this recording.
    pub version: String,
    /// The timestamp when the navigation session started.
    pub initial_timestamp: i64,
    /// Configuration of the navigation session.
    pub config: SerializableNavigationControllerConfig,
    /// The initial route assigned.
    pub initial_route: Route,
    /// Collection of events that occurred during the navigation session.
    pub events: Vec<NavigationRecordingEvent>,
}

/// Custom error type for navigation recording operations.
#[derive(Debug)]
#[cfg_attr(feature = "std", derive(thiserror::Error))]
pub enum NavigationRecordingError {
    #[error(transparent)]
    SerializationError(#[from] serde_json::Error),
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
    /// - `Err(NavigationRecordingError)` - If there was an error during JSON serialization
    pub fn to_json(&self) -> Result<String, NavigationRecordingError> {
        serde_json::to_string(self).map_err(NavigationRecordingError::SerializationError)
    }

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

    pub fn add_event(
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
