use chrono::Utc;
use serde::{Deserialize, Serialize};

use crate::{
    models::Route,
    navigation_controller::models::{
        NavigationControllerConfig, NavigationRecordingEvent,
        SerializableNavigationControllerConfig,
    },
};

/// A builder for serializing a navigation recording.
#[derive(Serialize, Deserialize, Clone)]
pub struct NavigationRecordingMetadata {
    /// Version of Ferrostar used.
    pub version: String,
    /// Initial timestamp of the recording.
    pub initial_timestamp: i64,
    /// Configuration of the navigation controller.
    pub config: SerializableNavigationControllerConfig,
    /// Initial route used during navigation.
    pub initial_route: Route,
}

impl NavigationRecordingMetadata {
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

/// A navigation session recording.
///
/// Internally this contains the full event stream.
// TODO: Hints for how you would typically use / interact with this? You can link to other types (and functions) by the way like [`NavigationReplay`] :)  and a list of navigation events.
#[derive(Serialize, Deserialize, Clone)]
pub struct NavigationRecording {
    #[serde(flatten)]
    pub recording: NavigationRecordingMetadata,
    /// List of navigation events in order of occurrence.
    pub events: Vec<NavigationRecordingEvent>,
}

impl NavigationRecording {
    /// Deserializes a previously saved navigation recording from a JSON string.
    pub fn from_json(json: &str) -> Self {
        serde_json::from_str(json)
            .map_err(|e| RecordingError::SerializationError {
                error: e.to_string(),
            })
            .unwrap()
    }
}

/// A session recording error.
#[derive(Debug, Serialize)]
#[cfg_attr(feature = "uniffi", derive(uniffi::Error))]
#[cfg_attr(feature = "std", derive(thiserror::Error))]
pub enum RecordingError {
    /// Error during serialization.
    #[cfg_attr(
        feature = "std",
        error("Error serializing navigation recording: {error}.")
    )]
    SerializationError { error: String },

    /// Recording is not enabled for this controller.
    #[cfg_attr(
        feature = "std",
        error("Recording is not enabled for this controller.")
    )]
    RecordingNotEnabled,
}
