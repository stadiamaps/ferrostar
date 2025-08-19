//! Module for recording and replaying navigation sessions.

use crate::models::Route;
use crate::navigation_controller::models::{
    NavigationControllerConfig, NavigationRecordingEvent, SerializableNavigationControllerConfig,
};
use chrono::Utc;
use serde::{Deserialize, Serialize};

#[cfg(feature = "wasm-bindgen")]
use wasm_bindgen::prelude::*;

/// A builder struct used to construct a recording of navigation events.
/// This struct encapsulates necessary data for initializing and configuring
/// a navigation recording process.
#[derive(Serialize, Deserialize, Clone)]
pub struct NavigationRecordingBuilder {
    pub version: String,
    pub initial_timestamp: i64,
    pub config: SerializableNavigationControllerConfig,
    pub initial_route: Route,
}

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

/// Represents a navigation recording, which consists of a builder containing metadata
/// and a list of navigation events.
#[derive(Serialize, Deserialize, Clone)]
pub struct NavigationRecording {
    #[serde(flatten)]
    recording: NavigationRecordingBuilder,
    events: Vec<NavigationRecordingEvent>,
}

impl NavigationRecording {
    /// Converts a JSON string slice into an instance of the type implementing this method.
    pub fn from_json(json: &str) -> Self {
        serde_json::from_str(json)
            .map_err(|e| RecordingError::SerializationError {
                error: e.to_string(),
            })
            .unwrap()
    }
}

/// Enum `RecordingError` represents the possible errors that can occur during
/// the recording process in a navigation recording system.
///
/// # Variants
///
/// - `SerializationError`
///   Indicates an error occurred during the serialization of a recording.
///
/// - `RecordingNotEnabled`
///   Indicates that recording is not enabled for a specific controller.
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

/// A struct that wraps around `NavigationRecording` to provide replay functionality.
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
pub struct NavigationReplay(NavigationRecording);

impl NavigationReplay {
    pub fn new(json: &str) -> Self {
        Self(NavigationRecording::from_json(json))
    }

    /// Retrieves the next navigation recording event based on the provided current index.
    /// Returns `None`, if there is no next event.
    pub fn get_next_event(&self, current_index: u64) -> Option<NavigationRecordingEvent> {
        match self.0.events.get(current_index as usize) {
            Some(event) => Some(event.clone()),
            None => None,
        }
    }

    pub fn get_initial_timestamp(&self) -> i64 {
        self.0.recording.initial_timestamp
    }

    pub fn get_initial_route(&self) -> Route {
        self.0.recording.initial_route.clone()
    }
}

/// A WebAssembly-compatible wrapper for `NavigationReplay` that exposes its functionality as a JavaScript object.
/// This wrapper is required because `NavigationReplay` cannot be directly converted to a JavaScript object
/// and requires serialization/deserialization of its methods' inputs and outputs.
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

    #[wasm_bindgen(js_name = getNextEvent)]
    pub fn get_next_event(&self, current_index: JsValue) -> Result<JsValue, JsValue> {
        let current_index: u64 = serde_wasm_bindgen::from_value(current_index)?;
        let next_event = self.0.get_next_event(current_index);
        serde_wasm_bindgen::to_value(&next_event)
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
