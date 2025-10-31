use crate::{
    models::Route,
    navigation_session::recording::models::{NavigationRecording, NavigationRecordingEvent},
};

#[cfg(feature = "wasm-bindgen")]
use wasm_bindgen::{JsValue, prelude::wasm_bindgen};

/// A wrapper around `NavigationRecording` to facilitate replaying the event stream.
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
pub struct NavigationReplay(NavigationRecording);

impl NavigationReplay {
    pub fn new(json: &str) -> Self {
        Self(NavigationRecording::from_json(json))
    }

    /// Retrieves the next navigation recording event at a specific index.
    ///
    /// Returns `None`, if there is no such event.
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

/// A WebAssembly-compatible wrapper for `NavigationReplay` that exposes its functionality as a JavaScript object.
///
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
