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

    /// Exports a `MessagePack` snapshot without creating a JSON string or cloning the event stream.
    ///
    /// The recording remains in memory, and the returned buffer contains the complete snapshot.
    /// Event updates wait until serialization finishes; prefer exporting after stopping navigation.
    pub fn get_recording_msgpack(&self) -> Result<Vec<u8>, RecordingError> {
        let events = self
            .events
            .lock()
            .map_err(|_| RecordingError::SerializationError {
                error: "recording event lock is poisoned".into(),
            })?;
        self.recording.to_msgpack(&events)
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
    fn recorded_session_exports_messagepack_without_foreign_observers() {
        let route = TestRoute::ValhallaSelfIntersecting.first_route();
        let config = get_test_navigation_controller_config(get_test_step_advance_condition(0));
        let recorder = Arc::new(NavigationRecorder::new(route.clone(), config.clone()));
        let session = crate::navigation_session::create_recorded_navigation_session(
            route.clone(),
            config,
            recorder.clone(),
        );
        let _ = test_full_route_state_snapshot(route, session);

        let bytes = recorder.get_recording_msgpack().unwrap();
        let decoded = NavigationRecording::try_from_msgpack(&bytes).unwrap();
        assert!(!decoded.events.is_empty());
        let json = serde_json::to_value(NavigationRecording {
            recording: recorder.recording.clone(),
            events: recorder.get_events(),
        })
        .unwrap();
        assert_json_diff::assert_json_eq!(serde_json::to_value(decoded).unwrap(), json);
        assert_eq!(
            rmp_serde::from_slice::<serde_json::Value>(&bytes).unwrap(),
            json
        );
    }

    #[test]
    fn messagepack_preserves_the_json_schema() {
        let canonical = include_str!("../../fixtures/recording_canonical.json");
        let recording = NavigationRecording::try_from_json(canonical).unwrap();
        let bytes = recording.recording.to_msgpack(&recording.events).unwrap();
        let expected: serde_json::Value = serde_json::from_str(canonical).unwrap();

        assert_eq!(
            bytes,
            include_bytes!("../../fixtures/recording_canonical.msgpack")
        );

        // Decode without Ferrostar's types too, to catch UUID or enum representation differences.
        assert_eq!(
            rmp_serde::from_slice::<serde_json::Value>(&bytes).unwrap(),
            expected
        );
        assert_eq!(
            serde_json::to_value(NavigationRecording::try_from_msgpack(&bytes).unwrap()).unwrap(),
            expected
        );
    }

    #[test]
    fn messagepack_handles_empty_recordings_and_unicode() {
        let mut recording = NavigationRecording::try_from_json(include_str!(
            "../../fixtures/recording_canonical.json"
        ))
        .unwrap();
        recording.recording.version = "نسخة / 東京 / 🚲".into();
        recording.events.clear();

        let bytes = recording.recording.to_msgpack(&recording.events).unwrap();
        assert_eq!(
            serde_json::to_value(NavigationRecording::try_from_msgpack(&bytes).unwrap()).unwrap(),
            serde_json::to_value(recording).unwrap()
        );
    }

    #[test]
    fn messagepack_rejects_invalid_truncated_and_trailing_data() {
        let recording = NavigationRecording::try_from_json(include_str!(
            "../../fixtures/recording_canonical.json"
        ))
        .unwrap();
        let bytes = recording.recording.to_msgpack(&recording.events).unwrap();
        for invalid in [
            &[][..],
            &[0xc1],
            &[0xc0],
            &bytes[..1],
            &bytes[..bytes.len() / 2],
            &bytes[..bytes.len() - 1],
        ] {
            assert!(matches!(
                NavigationRecording::try_from_msgpack(invalid),
                Err(RecordingError::DeserializationError { .. })
            ));
        }
        let mut trailing = bytes;
        trailing.push(0xc0);
        let error = NavigationRecording::try_from_msgpack(&trailing)
            .err()
            .unwrap();
        assert!(error.to_string().contains("trailing bytes"));
    }

    #[test]
    fn messagepack_reports_a_poisoned_event_lock() {
        let route = TestRoute::ValhallaSelfIntersecting.first_route();
        let config = get_test_navigation_controller_config(get_test_step_advance_condition(0));
        let recorder = Arc::new(NavigationRecorder::new(route, config));
        let writer = recorder.clone();
        assert!(
            std::thread::spawn(move || {
                let _events = writer.events.lock().unwrap();
                panic!("interrupted event update");
            })
            .join()
            .is_err()
        );
        let error = recorder.get_recording_msgpack().unwrap_err();
        assert!(matches!(error, RecordingError::SerializationError { .. }));
        assert!(
            error
                .to_string()
                .contains("recording event lock is poisoned")
        );
    }

    #[test]
    fn messagepack_preserves_events_for_a_long_session() {
        let recording = NavigationRecording::try_from_json(include_str!(
            "../../fixtures/recording_canonical.json"
        ))
        .unwrap();
        // More than two hours of one event per second, without a gigabyte-sized test allocation.
        let events: Vec<_> = (0..7_201)
            .map(|index| {
                let mut event = recording.events[0].clone();
                event.timestamp = recording.recording.initial_timestamp + index * 1_000;
                event
            })
            .collect();
        let recorder = NavigationRecorder {
            recording: recording.recording,
            events: std::sync::Mutex::new(events),
        };
        let bytes = recorder.get_recording_msgpack().unwrap();
        let decoded = NavigationRecording::try_from_msgpack(&bytes).unwrap();
        assert_eq!(decoded.events.len(), 7_201);
        for (index, event) in decoded.events.iter().enumerate() {
            assert_eq!(
                event.timestamp,
                decoded.recording.initial_timestamp + i64::try_from(index).unwrap() * 1_000
            );
        }
        assert_eq!(recorder.get_events().len(), decoded.events.len());
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
