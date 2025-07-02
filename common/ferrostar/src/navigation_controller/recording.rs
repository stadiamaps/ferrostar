use crate::models::{Route, UserLocation};
use crate::navigation_controller::models::{
    InitialNavigationState, NavigationControllerConfig, NavigationRecordingEvent,
    NavigationRecordingEventData, TripState,
};
use chrono::Utc;
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
pub struct NavigationRecording {
    /// Version of Ferrostar that created this recording.
    pub version: String,
    /// The timestamp when the navigation session started.
    pub initial_timestamp: i64,
    /// Configuration of the navigation session.
    pub route_config: NavigationControllerConfig,
    /// The initial state of the navigation session.
    pub initial_state: InitialNavigationState,
    /// Collection of events that occurred during the navigation session.
    pub events: Vec<NavigationRecordingEvent>,
}

/// Custom error type for navigation recording operations.
#[derive(Debug)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Error))]
#[cfg_attr(feature = "std", derive(thiserror::Error))]
pub enum NavigationRecordingError {
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

/// Functionality for the navigation controller that is exported.
#[cfg_attr(feature = "uniffi", uniffi::export)]
impl NavigationRecording {
    /// Serialize the recording to a pretty JSON format.
    /// Returns a Result with the JSON string or an error.
    pub fn to_json(&self) -> Result<String, NavigationRecordingError> {
        serde_json::to_string_pretty(self).map_err(|error| NavigationRecordingError::from(error))
    }
}

/// Functionality for the navigation controller that is not exported.
impl NavigationRecording {
    /// Creates a new navigation recorder with route configuration and initial state.
    pub fn new(
        route_config: NavigationControllerConfig,
        initial_state: InitialNavigationState,
    ) -> Self {
        Self {
            version: env!("CARGO_PKG_VERSION").to_string(),
            initial_timestamp: Utc::now().timestamp(),
            route_config,
            initial_state,
            events: Vec::new(),
        }
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
