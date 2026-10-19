use crate::{
    models::{Route, UserLocation},
    navigation_controller::models::{NavState, NavigationControllerConfig},
    navigation_session::{
        NavigationObserver,
        recording::models::{
            NavigationRecordingEvent, NavigationRecordingMetadata, RecordingError,
        },
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

#[cfg_attr(feature = "uniffi", uniffi::export)]
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

    pub fn get_recording_json(&self) -> Result<String, RecordingError> {
        let events = self.get_events();
        self.recording.to_json(events)
    }
}

#[cfg_attr(feature = "uniffi", uniffi::export)]
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

    fn on_route_available(&self, #[allow(unused_variables)] route: Route) {
        // TODO: We could capture the route on the recording if desired.
    }
}

#[cfg(test)]
mod tests {
    use std::sync::Arc;

    use crate::routing_adapters::osrm::models::OsrmWaypointProperties;
    use crate::test_utils::{TestRoute, redact_properties};
    use crate::{
        navigation_controller::{
            NavigationController,
            test_helpers::{
                get_test_navigation_controller_config, get_test_step_advance_condition,
                nav_controller_insta_settings,
            },
        },
        navigation_session::recording::models::{NavigationRecording, RecordingError},
        navigation_session::{
            NavigationSession, recording::NavigationRecorder,
            test_helpers::test_full_route_state_snapshot,
        },
    };

    #[test]
    fn test_recording_serialization() {
        nav_controller_insta_settings().bind(|| {
            let route = TestRoute::ValhallaSelfIntersecting.first_route();
            let config = get_test_navigation_controller_config(get_test_step_advance_condition(0));
            let recorder = Arc::new(NavigationRecorder::new(route.clone(), config.clone()));
            let session = NavigationSession::new(
                Arc::new(NavigationController::new(route.clone(), config)),
                vec![recorder.clone()],
            );
            let _ = test_full_route_state_snapshot(route, session);

            let json = recorder.get_recording_json().unwrap();
            let value: serde_json::Value = serde_json::from_str(&json).unwrap();
            insta::assert_yaml_snapshot!(value, {
                ".**.utteranceId" => "[uuid]",
                ".**.remainingWaypoints[].properties" => insta::dynamic_redaction(redact_properties::<OsrmWaypointProperties>),
                ".**.remaining_waypoints[].properties" => insta::dynamic_redaction(redact_properties::<OsrmWaypointProperties>),
                ".**.waypoints[].properties" => insta::dynamic_redaction(redact_properties::<OsrmWaypointProperties>),
            });
        });
    }

    #[test]
    fn recording_deserializes_legacy_and_canonical_formats() {
        let legacy = include_str!("../../fixtures/recording_legacy_native.json");
        let canonical = include_str!("../../fixtures/recording_canonical.json");

        let legacy_recording = NavigationRecording::try_from_json(legacy).unwrap();
        let canonical_recording = NavigationRecording::try_from_json(canonical).unwrap();
        let expected: serde_json::Value = serde_json::from_str(canonical).unwrap();

        assert_eq!(serde_json::to_value(legacy_recording).unwrap(), expected);
        assert_eq!(serde_json::to_value(canonical_recording).unwrap(), expected);
    }

    #[test]
    fn recording_deserialization_preserves_serde_error_context() {
        let error = NavigationRecording::try_from_json("{}").err().unwrap();

        if !matches!(error, RecordingError::DeserializationError { .. }) {
            panic!("expected a deserialization error");
        };
    }

    #[test]
    fn recording_deserialization_rejects_duplicate_aliases() {
        let canonical = include_str!("../../fixtures/recording_canonical.json");
        let mut value: serde_json::Value = serde_json::from_str(canonical).unwrap();
        value
            .pointer_mut("/events/0/event_data/StateUpdate/trip_state/Navigating/progress")
            .unwrap()
            .as_object_mut()
            .unwrap()
            .insert("distance_to_next_maneuver".into(), 1.0.into());

        let error = NavigationRecording::try_from_json(&value.to_string())
            .err()
            .unwrap();
        assert!(error.to_string().contains("duplicate field"));
    }

    #[test]
    fn recording_deserialization_rejects_invalid_legacy_timestamp() {
        let legacy = include_str!("../../fixtures/recording_legacy_native.json");
        let mut value: serde_json::Value = serde_json::from_str(legacy).unwrap();
        *value
            .pointer_mut(
                "/events/0/event_data/StateUpdate/trip_state/Navigating/user_location/\
                 timestamp/nanos_since_epoch",
            )
            .unwrap() = 1_000_000_000_u64.into();

        let error = NavigationRecording::try_from_json(&value.to_string())
            .err()
            .unwrap();
        assert!(
            error
                .to_string()
                .contains("nanos_since_epoch must be less than 1000000000")
        );
    }

    #[test]
    fn recording_deserialization_rejects_overflowing_legacy_timestamp() {
        let legacy = include_str!("../../fixtures/recording_legacy_native.json");
        let mut value: serde_json::Value = serde_json::from_str(legacy).unwrap();
        *value
            .pointer_mut(
                "/events/0/event_data/StateUpdate/trip_state/Navigating/user_location/\
                 timestamp/secs_since_epoch",
            )
            .unwrap() = u64::MAX.into();

        let error = NavigationRecording::try_from_json(&value.to_string())
            .err()
            .unwrap();
        assert!(
            error
                .to_string()
                .contains("system time exceeds epoch milliseconds")
        );
    }
}
