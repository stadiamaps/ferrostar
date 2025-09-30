//! Specialized navigation session wrappers for JavaScript/WebAssembly.
//!
//! This module contains specialized versions of NavigationSession optimized for web usage,
//! where trait object complexity needs to be avoided and specific use cases can be
//! directly implemented for better performance and usability.

use std::sync::Arc;

use crate::{
    models::{Route, UserLocation},
    navigation_controller::{
        models::{SerializableNavState, SerializableNavigationControllerConfig},
        NavigationController, Navigator,
    },
    navigation_session::{recording::NavigationRecorder, NavigationObserver, NavigationSession},
};

#[cfg(feature = "wasm-bindgen")]
use wasm_bindgen::{prelude::wasm_bindgen, JsValue};

/// JavaScript wrapper for `NavigationSession` (simple version).
/// This wrapper provides basic navigation functionality without observers.
#[cfg(feature = "wasm-bindgen")]
#[wasm_bindgen(js_name = NavigationSession)]
pub struct JsNavigationSession {
    session: NavigationSession,
}

#[cfg(feature = "wasm-bindgen")]
#[wasm_bindgen(js_class = NavigationSession)]
impl JsNavigationSession {
    #[wasm_bindgen(constructor)]
    pub fn new(route: JsValue, config: JsValue) -> Result<JsNavigationSession, JsValue> {
        let route: Route = serde_wasm_bindgen::from_value(route)?;
        let config: SerializableNavigationControllerConfig =
            serde_wasm_bindgen::from_value(config)?;

        let controller = Arc::new(NavigationController::new(route, config.into()));
        let session = NavigationSession::new(controller, vec![]);

        Ok(JsNavigationSession { session })
    }

    #[wasm_bindgen(js_name = getInitialState)]
    pub fn get_initial_state(&self, location: JsValue) -> Result<JsValue, JsValue> {
        let location: UserLocation = serde_wasm_bindgen::from_value(location)?;
        let nav_state = self.session.get_initial_state(location);
        let result: SerializableNavState = nav_state.into();

        serde_wasm_bindgen::to_value(&result).map_err(|e| JsValue::from_str(&format!("{:?}", e)))
    }

    #[wasm_bindgen(js_name = advanceToNextStep)]
    pub fn advance_to_next_step(&self, state: JsValue) -> Result<JsValue, JsValue> {
        let state: SerializableNavState = serde_wasm_bindgen::from_value(state)?;
        let new_state = self.session.advance_to_next_step(state.into());

        serde_wasm_bindgen::to_value(&SerializableNavState::from(new_state))
            .map_err(|e| JsValue::from_str(&format!("{:?}", e)))
    }

    #[wasm_bindgen(js_name = updateUserLocation)]
    pub fn update_user_location(
        &self,
        location: JsValue,
        state: JsValue,
    ) -> Result<JsValue, JsValue> {
        let location: UserLocation = serde_wasm_bindgen::from_value(location)?;
        let state: SerializableNavState = serde_wasm_bindgen::from_value(state)?;
        let new_state = self.session.update_user_location(location, state.into());

        serde_wasm_bindgen::to_value(&SerializableNavState::from(new_state))
            .map_err(|e| JsValue::from_str(&format!("{:?}", e)))
    }
}

/// JavaScript wrapper for `NavigationSession` with recording capabilities.
/// This version includes a NavigationRecorder observer and provides direct access to recording functionality.
#[cfg(feature = "wasm-bindgen")]
#[wasm_bindgen(js_name = NavigationSessionRecording)]
pub struct JsNavigationSessionRecording {
    session: NavigationSession,
    recorder: Arc<NavigationRecorder>,
}

#[cfg(feature = "wasm-bindgen")]
#[wasm_bindgen(js_class = NavigationSessionRecording)]
impl JsNavigationSessionRecording {
    #[wasm_bindgen(constructor)]
    pub fn new(route: JsValue, config: JsValue) -> Result<JsNavigationSessionRecording, JsValue> {
        let route: Route = serde_wasm_bindgen::from_value(route)?;
        let config: SerializableNavigationControllerConfig =
            serde_wasm_bindgen::from_value(config)?;

        let controller = Arc::new(NavigationController::new(
            route.clone(),
            config.clone().into(),
        ));

        // Create a single recorder instance that will be shared
        let recorder = Arc::new(NavigationRecorder::new(route, config.into()));

        // Use the same recorder instance for both observer and direct access
        let observers: Vec<Arc<dyn NavigationObserver>> = vec![recorder.clone()];
        let session = NavigationSession::new(controller, observers);

        Ok(JsNavigationSessionRecording { session, recorder })
    }

    #[wasm_bindgen(js_name = getInitialState)]
    pub fn get_initial_state(&self, location: JsValue) -> Result<JsValue, JsValue> {
        let location: UserLocation = serde_wasm_bindgen::from_value(location)?;
        let nav_state = self.session.get_initial_state(location);
        let result: SerializableNavState = nav_state.into();

        serde_wasm_bindgen::to_value(&result).map_err(|e| JsValue::from_str(&format!("{:?}", e)))
    }

    #[wasm_bindgen(js_name = advanceToNextStep)]
    pub fn advance_to_next_step(&self, state: JsValue) -> Result<JsValue, JsValue> {
        let state: SerializableNavState = serde_wasm_bindgen::from_value(state)?;
        let new_state = self.session.advance_to_next_step(state.into());

        serde_wasm_bindgen::to_value(&SerializableNavState::from(new_state))
            .map_err(|e| JsValue::from_str(&format!("{:?}", e)))
    }

    #[wasm_bindgen(js_name = updateUserLocation)]
    pub fn update_user_location(
        &self,
        location: JsValue,
        state: JsValue,
    ) -> Result<JsValue, JsValue> {
        let location: UserLocation = serde_wasm_bindgen::from_value(location)?;
        let state: SerializableNavState = serde_wasm_bindgen::from_value(state)?;
        let new_state = self.session.update_user_location(location, state.into());

        serde_wasm_bindgen::to_value(&SerializableNavState::from(new_state))
            .map_err(|e| JsValue::from_str(&format!("{:?}", e)))
    }

    #[wasm_bindgen(js_name = getRecording)]
    pub fn get_recording(&self) -> Result<JsValue, JsValue> {
        let recording = self.recorder.get_recording();

        match recording {
            Ok(recording) => Ok(JsValue::from_str(&recording)),
            Err(e) => Err(JsValue::from_str(&format!("{:?}", e))),
        }
    }

    #[wasm_bindgen(js_name = getEvents)]
    pub fn get_events(&self) -> Result<JsValue, JsValue> {
        let events = self.recorder.get_events();
        serde_wasm_bindgen::to_value(&events).map_err(|e| JsValue::from_str(&format!("{:?}", e)))
    }
}
