use crate::{
    models::{Route, UserLocation},
    navigation_controller::models::{NavState, NavigationControllerConfig},
    navigation_session::{
        recording::models::{
            NavigationRecordingEvent, NavigationRecordingMetadata, RecordingError,
        },
        NavigationObserver,
    },
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

    pub fn get_recording(&self) -> Result<String, RecordingError> {
        let events = self.get_events();
        self.recording.to_json(events)
    }
}

#[cfg_attr(feature = "uniffi", uniffi::export)]
impl NavigationObserver for NavigationRecorder {
    fn on_route_available(&self, #[allow(unused_variables)] route: Route) {
        // TODO: We could capture the route on the recording if desired.
    }

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
    use std::sync::Arc;

    use crate::{
        navigation_controller::{
            test_helpers::{
                get_test_navigation_controller_config, get_test_route,
                get_test_step_advance_condition, nav_controller_insta_settings, TestRoute,
            },
            NavigationController,
        },
        navigation_session::{
            recording::NavigationRecorder, test_helpers::test_full_route_state_snapshot,
            NavigationSession,
        },
    };

    #[test]
    fn test_recording_serialization() {
        nav_controller_insta_settings().bind(|| {
            let route = get_test_route(TestRoute::SelfIntersecting);
            let config = get_test_navigation_controller_config(get_test_step_advance_condition(0));
            let recorder = Arc::new(NavigationRecorder::new(route.clone(), config.clone()));
            let session = NavigationSession::new(
                Arc::new(NavigationController::new(route.clone(), config)),
                vec![recorder.clone()],
            );
            let _ = test_full_route_state_snapshot(route, session);

            let json = recorder.get_recording().unwrap();
            let value: serde_json::Value = serde_json::from_str(&json).unwrap();
            insta::assert_yaml_snapshot!(value);
        })
    }
}
