use crate::models::Route;
use crate::navigation_controller::models::{
    NavState, NavigationControllerConfig, NavigationRecordingEvent, NavigationRecordingEventData,
    TripState,
};
use chrono::Utc;
pub struct NavigationRecording {
    /// Version of Ferrostar that created this recording.
    pub version: String,
    /// The timestamp when the navigation session started.
    pub initial_timestamp: i64,
    /// Configuration of the navigation session.
    pub config: NavigationControllerConfig,
    /// The initial route assigned.
    pub initial_route: Route,
    /// Collection of events that occurred during the navigation session.
    pub events: Vec<NavigationRecordingEvent>,
}

/// Functionality for the navigation controller that is exported. (or will be exported)
impl NavigationRecording {
    /// Creates a new navigation recorder with route configuration and initial state.
    pub fn new(config: NavigationControllerConfig, initial_route: Route) -> Self {
        Self {
            version: env!("CARGO_PKG_VERSION").to_string(),
            initial_timestamp: Utc::now().timestamp(),
            config,
            initial_route,
            events: Vec::new(),
        }
    }

    pub fn record_nav_state_update(
        &self,
        old_state: NavState,
        new_state: NavState,
    ) -> Vec<NavigationRecordingEvent> {
        match old_state.recording_events {
            Some(old_events) => Self::add_event(
                old_events,
                NavigationRecordingEventData::NavStateUpdate {
                    nav_state: new_state.into(),
                },
            ),
            None => Vec::new(),
        }
    }

    pub fn add_event(
        mut old_events: Vec<NavigationRecordingEvent>,
        new_event_data: NavigationRecordingEventData,
    ) -> Vec<NavigationRecordingEvent> {
        old_events.push(NavigationRecordingEvent {
            timestamp: Utc::now().timestamp(),
            event_data: new_event_data,
        });
        old_events
    }
}
