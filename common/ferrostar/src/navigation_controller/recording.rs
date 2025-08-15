use crate::models::Route;
use crate::navigation_controller::models::{
    NavigationControllerConfig, NavigationRecordingEvent, SerializableNavigationControllerConfig,
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
#[derive(Clone, Serialize, Deserialize)]
pub struct NavigationRecordingBuilder {
    pub version: String,
    pub initial_timestamp: i64,
    pub config: SerializableNavigationControllerConfig,
    pub initial_route: Route,
}

/// Functionality for the navigation controller that is not exported.
impl NavigationRecordingBuilder {
    /// Creates a new navigation recorder with route configuration and initial state.
    pub fn new(config: NavigationControllerConfig, initial_route: Route) -> Self {
        Self {
            version: env!("CARGO_PKG_VERSION").to_string(),
            initial_timestamp: Utc::now().timestamp_millis(),
            config: SerializableNavigationControllerConfig::from(config),
            initial_route,
        }
    }

    /// Serializes the navigation recording to a JSON string.
    ///
    /// # Returns
    ///
    /// - `Ok(String)` - A JSON string representation of the navigation recording
    /// - `Err(RecordingError)` - If there was an error during JSON serialization
    pub fn to_json(&self, events: Vec<NavigationRecordingEvent>) -> Result<String, RecordingError> {
        let recording = NavigationRecording {
            recording: self.clone(),
            events,
        };
        serde_json::to_string(&recording).map_err(|e| RecordingError::SerializationError {
            error: e.to_string(),
        })
    }
}

#[derive(Serialize, Deserialize, Clone)]
pub struct NavigationRecording {
    #[serde(flatten)]
    recording: NavigationRecordingBuilder,
    events: Vec<NavigationRecordingEvent>,
}

/// Custom error type for navigation recording operations.
#[derive(Debug, Serialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Error))]
#[cfg_attr(feature = "std", derive(thiserror::Error))]
pub enum RecordingError {
    #[cfg_attr(
        feature = "std",
        error("Error serializing navigation recording: {error}.")
    )]
    SerializationError { error: String },
    #[cfg_attr(
        feature = "std",
        error("Recording is not enabled for this controller.")
    )]
    RecordingNotEnabled,
}
