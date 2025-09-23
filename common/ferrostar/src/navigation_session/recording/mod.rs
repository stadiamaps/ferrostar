use crate::{
    models::{Route, UserLocation},
    navigation_controller::models::{
        NavState, NavigationControllerConfig, NavigationRecordingEvent,
    },
    navigation_session::{recording::models::NavigationRecordingMetadata, NavigationObserver},
};
use std::sync::Mutex;

pub mod models;
pub mod replay;

#[cfg_attr(feature = "uniffi", derive(uniffi::Object))]
pub struct NavigationRecorder {
    pub recording: NavigationRecordingMetadata,
    events: Mutex<Vec<NavigationRecordingEvent>>,
}

impl NavigationRecorder {
    #[cfg_attr(feature = "uniffi", uniffi::constructor)]
    pub fn new(route: Route, config: NavigationControllerConfig) -> Self {
        let recording = NavigationRecordingMetadata::new(config, route);
        Self {
            recording,
            events: Mutex::new(Vec::new()),
        }
    }
}

#[cfg_attr(feature = "uniffi", uniffi::export)]
impl NavigationRecorder {
    pub fn get_events(&self) -> Vec<NavigationRecordingEvent> {
        self.events.lock().unwrap().clone()
    }

    pub fn get_recording(&self) -> String {
        let events = self.get_events();
        serde_json::to_string(&events).unwrap()
    }
}

impl NavigationObserver for NavigationRecorder {
    fn on_get_initial_state(&self, state: NavState) {
        let event = NavigationRecordingEvent::state_update(state.into());
        if let Ok(mut events) = self.events.lock() {
            events.push(event);
        }
    }

    fn on_user_location_update(
        &self,
        // The users location is captured in the NavState
        #[allow(unused_variables)] location: UserLocation,
        state: NavState,
    ) {
        let event = NavigationRecordingEvent::state_update(state.into());
        if let Ok(mut events) = self.events.lock() {
            events.push(event);
        }
    }

    fn on_advance_to_next_step(&self, state: NavState) {
        let event = NavigationRecordingEvent::state_update(state.into());
        if let Ok(mut events) = self.events.lock() {
            events.push(event);
        }
    }
}

#[cfg(test)]
mod tests {

    #[test]
    fn test_recording_serialization() {
        todo!("test the recorder")
    }
}
