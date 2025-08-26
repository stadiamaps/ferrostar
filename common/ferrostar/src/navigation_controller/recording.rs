use crate::models::Route;
use crate::navigation_controller::models::{
    NavigationControllerConfig, NavigationRecordingEvent, SerializableNavigationControllerConfig,
};
use chrono::Utc;
use serde::{Deserialize, Serialize};

#[cfg(feature = "wasm-bindgen")]
use wasm_bindgen::prelude::*;

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

impl NavigationRecording {
    pub fn from_json(json: &str) -> Self {
        serde_json::from_str(json)
            .map_err(|e| RecordingError::SerializationError {
                error: e.to_string(),
            })
            .unwrap()
    }
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

#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
pub struct NavigationReplay(NavigationRecording);

impl NavigationReplay {
    pub fn new(json: &str) -> Self {
        Self(NavigationRecording::from_json(json))
    }

    pub fn get_event_by_index(&self, current_index: u64) -> Option<&NavigationRecordingEvent> {
        match self.0.events.get(current_index as usize) {
            Some(event) => Some(event),
            None => None,
        }
    }

    pub fn get_all_events(&self) -> &[NavigationRecordingEvent] {
        &self.0.events
    }

    pub fn get_total_duration(&self) -> i64 {
        if self.0.events.is_empty() {
            return 0;
        }
        let first_event = self.0.events.first().unwrap();
        let last_event = self.0.events.last().unwrap();
        last_event.timestamp() - first_event.timestamp()
    }

    pub fn get_initial_timestamp(&self) -> i64 {
        self.0.recording.initial_timestamp
    }

    pub fn get_initial_route(&self) -> &Route {
        &self.0.recording.initial_route
    }
}

#[cfg(feature = "wasm-bindgen")]
#[wasm_bindgen(js_name = NavigationReplay)]
pub struct JsNavigationReplay(NavigationReplay);

#[cfg(feature = "wasm-bindgen")]
#[wasm_bindgen(js_class = NavigationReplay)]
impl JsNavigationReplay {
    #[wasm_bindgen(constructor)]
    pub fn new(json: JsValue) -> Result<JsNavigationReplay, JsValue> {
        let json: String = serde_wasm_bindgen::from_value(json)?;

        Ok(JsNavigationReplay(NavigationReplay::new(&json)))
    }

    #[wasm_bindgen(js_name = getEventByIndex)]
    pub fn get_event_by_index(&self, current_index: JsValue) -> Result<JsValue, JsValue> {
        let current_index: u64 = serde_wasm_bindgen::from_value(current_index)?;
        let next_event = self.0.get_event_by_index(current_index);
        serde_wasm_bindgen::to_value(&next_event)
            .map_err(|e| JsValue::from_str(&format!("{:?}", e)))
    }

    #[wasm_bindgen(js_name = getAllEvents)]
    pub fn get_all_events(&self) -> Result<JsValue, JsValue> {
        serde_wasm_bindgen::to_value(&self.0.get_all_events())
            .map_err(|e| JsValue::from_str(&format!("{:?}", e)))
    }

    #[wasm_bindgen(js_name = getTotalDuration)]
    pub fn get_total_duration(&self) -> Result<JsValue, JsValue> {
        serde_wasm_bindgen::to_value(&self.0.get_total_duration())
            .map_err(|e| JsValue::from_str(&format!("{:?}", e)))
    }

    #[wasm_bindgen(js_name = getInitialTimestamp)]
    pub fn get_initial_timestamp(&self) -> Result<JsValue, JsValue> {
        serde_wasm_bindgen::to_value(&self.0.get_initial_timestamp())
            .map_err(|e| JsValue::from_str(&format!("{:?}", e)))
    }

    #[wasm_bindgen(js_name = getInitialRoute)]
    pub fn get_initial_route(&self) -> Result<JsValue, JsValue> {
        serde_wasm_bindgen::to_value(&self.0.get_initial_route())
            .map_err(|e| JsValue::from_str(&format!("{:?}", e)))
    }
}
