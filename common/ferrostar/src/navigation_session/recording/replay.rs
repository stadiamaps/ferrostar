use crate::{
    models::Route,
    navigation_session::recording::models::{
        NavigationRecording, NavigationRecordingEvent, RecordingError,
    },
};

#[cfg(feature = "wasm-bindgen")]
use wasm_bindgen::{JsError, JsValue, prelude::wasm_bindgen};

/// A wrapper around `NavigationRecording` to facilitate replaying the event stream.
#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
pub struct NavigationReplay(NavigationRecording);

impl NavigationReplay {
    pub fn try_new(json: &str) -> Result<Self, RecordingError> {
        NavigationRecording::try_from_json(json).map(Self)
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
        let (Some(first_event), Some(last_event)) = (self.0.events.first(), self.0.events.last())
        else {
            return 0;
        };

        last_event
            .timestamp()
            .saturating_sub(first_event.timestamp())
            .max(0)
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
    pub fn new(json: String) -> Result<JsNavigationReplay, JsError> {
        NavigationReplay::try_new(&json)
            .map(JsNavigationReplay)
            .map_err(|error| JsError::new(&error.to_string()))
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

#[cfg(test)]
mod tests {
    use super::NavigationReplay;

    #[test]
    fn replay_handles_empty_and_regressing_event_timelines() {
        let canonical = include_str!("../../fixtures/recording_canonical.json");
        let mut value: serde_json::Value = serde_json::from_str(canonical).unwrap();
        value["events"] = serde_json::json!([]);
        let replay = NavigationReplay::try_new(&value.to_string()).unwrap();
        assert_eq!(replay.get_total_duration(), 0);

        value["events"] = serde_json::json!([
            { "timestamp": i64::MAX, "event_data": { "RouteUpdate": {
                "route": value["initial_route"].clone()
            }}},
            { "timestamp": i64::MIN, "event_data": { "RouteUpdate": {
                "route": value["initial_route"].clone()
            }}}
        ]);
        let replay = NavigationReplay::try_new(&value.to_string()).unwrap();
        assert_eq!(replay.get_total_duration(), 0);
    }
}

#[cfg(all(test, target_arch = "wasm32", feature = "wasm-bindgen"))]
mod wasm_tests {
    use js_sys::Error;
    use wasm_bindgen::{JsCast, JsValue};
    use wasm_bindgen_test::wasm_bindgen_test;

    use super::JsNavigationReplay;

    wasm_bindgen_test::wasm_bindgen_test_configure!(run_in_browser);

    #[wasm_bindgen_test]
    fn replay_constructor_accepts_supported_recording_formats() {
        let legacy = include_str!("../../fixtures/recording_legacy_native.json");
        let canonical = include_str!("../../fixtures/recording_canonical.json");

        assert!(JsNavigationReplay::new(legacy.into()).is_ok());
        assert!(JsNavigationReplay::new(canonical.into()).is_ok());
    }

    #[wasm_bindgen_test]
    fn replay_constructor_throws_a_javascript_error() {
        let error = JsNavigationReplay::new("{}".into()).err().unwrap();
        let value: JsValue = error.into();

        assert!(value.is_instance_of::<Error>());
        let error: Error = value.unchecked_into();
        assert!(
            String::from(error.message())
                .contains("failed to deserialize navigation recording: missing field")
        );
    }
}
