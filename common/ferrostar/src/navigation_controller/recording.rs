use crate::models::{Route, UserLocation};
use crate::navigation_controller::models::{
    JsNavigationControllerConfig, NavigationControllerConfig, NavigationRecordingEvent,
    NavigationRecordingEventData, TripState,
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
    ///
    /// NOTE: This has to be the `JsNavigationControllerConfig` since `NavigationControllerConfig` can't be Serialized
    pub config: JsNavigationControllerConfig,
    /// The initial route assigned.
    pub initial_route: Route,
    /// Initial trip state.
    pub initial_trip_state: Option<TripState>,
    /// Collection of events that occurred during the navigation session.
    pub events: Vec<NavigationRecordingEvent>,
}

/// Custom error type for navigation recording operations.
#[derive(Debug)]
#[cfg_attr(feature = "std", derive(thiserror::Error))]
pub enum NavigationRecordingError {
    /// TODO: Converting error into string is not ideal
    #[cfg_attr(feature = "std", error("Serialization error: {error}."))]
    SerializationError { error: String },
}

/// Implement conversion from serde_json::Error to NavigationRecordingError.
impl From<serde_json::Error> for NavigationRecordingError {
    fn from(e: serde_json::Error) -> Self {
        NavigationRecordingError::SerializationError {
            error: e.to_string(),
        }
    }
}

/// Functionality for the navigation controller that is not exported.
impl NavigationRecording {
    /// Creates a new navigation recorder with route configuration and initial state.
    pub fn new(config: NavigationControllerConfig, initial_route: Route) -> Self {
        Self {
            version: env!("CARGO_PKG_VERSION").to_string(),
            initial_timestamp: Utc::now().timestamp(),
            config: JsNavigationControllerConfig::from(config),
            initial_route,
            initial_trip_state: None,
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
        serde_json::to_string(self).map_err(NavigationRecordingError::from)
    }

    /// Records a location update from the user during navigation.
    pub fn record_location_update(self, user_location: UserLocation) -> Self {
        self.add_event(NavigationRecordingEventData::LocationUpdate { user_location })
    }

    /// Records a trip state update during navigation.
    pub fn record_trip_state_update(self, trip_state: TripState) -> Self {
        self.add_event(NavigationRecordingEventData::TripStateUpdate { trip_state })
    }

    /// Records a route update during navigation.
    pub fn record_route_update(self, route: Route) -> Self {
        self.add_event(NavigationRecordingEventData::RouteUpdate { route })
    }

    /// Records an error that occurred during navigation.
    pub fn record_navigation_error(self, error_message: String) -> Self {
        self.add_event(NavigationRecordingEventData::Error { error_message })
    }

    /// Helper method to add an event to the recording.
    pub fn add_event(self, event_data: NavigationRecordingEventData) -> Self {
        let event = NavigationRecordingEvent {
            timestamp: Utc::now().timestamp(),
            event_data,
        };

        let mut new_recording = self;
        new_recording.events.push(event);

        new_recording
    }
}
